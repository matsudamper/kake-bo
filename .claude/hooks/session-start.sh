#!/bin/bash
set -euo pipefail

# Only run in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "Configuring Gradle proxy settings and downloading dependencies..."

python3 - <<'PYEOF'
import os, sys, hashlib, urllib.request, urllib.parse

proxy_url = os.environ.get('HTTPS_PROXY', '')
if not proxy_url:
    print("HTTPS_PROXY is not set; skipping Gradle proxy configuration")
    sys.exit(0)

parsed   = urllib.parse.urlparse(proxy_url)
host     = parsed.hostname
port     = str(parsed.port)
user     = parsed.username or ''
password = parsed.password or ''

gradle_home = os.path.expanduser('~/.gradle')
os.makedirs(gradle_home, exist_ok=True)

# ── プロキシ認証用 Gradle init スクリプト ─────────────────────────────────────
# foojay などが Authenticator を使えるようにする
init_d = os.path.join(gradle_home, 'init.d')
os.makedirs(init_d, exist_ok=True)
init_script = os.path.join(init_d, 'proxy-auth.gradle')
with open(init_script, 'w') as f:
    f.write(f"""import java.net.Authenticator
import java.net.PasswordAuthentication

def proxyUser = System.getProperty("https.proxyUser") ?: System.getProperty("http.proxyUser")
def proxyPassword = System.getProperty("https.proxyPassword") ?: System.getProperty("http.proxyPassword")

if (proxyUser && proxyPassword) {{
    Authenticator.setDefault(new Authenticator() {{
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {{
            if (getRequestorType() == Authenticator.RequestorType.PROXY) {{
                return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray())
            }}
            return null
        }}
    }})
}}
""")
print(f"Gradle init script written: {init_script}")

def download(url, dest_path, opener):
    print(f"Downloading {url} ...")
    with opener.open(url) as resp:
        total = int(resp.headers.get('Content-Length', 0))
        downloaded = 0
        with open(dest_path, 'wb') as f:
            while True:
                chunk = resp.read(65536)
                if not chunk:
                    break
                f.write(chunk)
                downloaded += len(chunk)
                if total:
                    pct = downloaded * 100 // total
                    print(f"\r  {pct}% ({downloaded}/{total} bytes)", end='', flush=True)
    print()

proxy_handler = urllib.request.ProxyHandler({'https': proxy_url, 'http': proxy_url})
opener = urllib.request.build_opener(proxy_handler)

# ── JDK 24 の truststore にプロキシ CA を追加 ─────────────────────────────────
# foojay がダウンロードした JDK 24 が使う cacerts に Anthropic TLS 検査 CA を追加する
import re, subprocess, tempfile

jdk24_cacerts = os.path.expanduser('~/.jdks/jdk-24/lib/security/cacerts')
keytool = os.path.expanduser('~/.jdks/jdk-24/bin/keytool')

if os.path.exists(keytool) and os.path.exists(jdk24_cacerts):
    sys_ca_bundle = '/etc/ssl/certs/ca-certificates.crt'
    with open(sys_ca_bundle) as f:
        bundle = f.read()
    pem_blocks = re.findall(r'-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----', bundle, re.DOTALL)
    for pem in pem_blocks:
        result = subprocess.run(['openssl', 'x509', '-noout', '-subject'], input=pem, capture_output=True, text=True)
        if 'Anthropic' not in result.stdout:
            continue
        cn_match = re.search(r'CN\s*=\s*([^\n,]+)', result.stdout)
        alias = cn_match.group(1).strip().lower().replace(' ', '-') if cn_match else 'anthropic-ca'
        # 既にインポート済みかチェック
        check = subprocess.run([keytool, '-list', '-alias', alias, '-keystore', jdk24_cacerts, '-storepass', 'changeit'],
                               capture_output=True, text=True)
        if check.returncode == 0:
            print(f"CA already imported: {alias}")
            continue
        with tempfile.NamedTemporaryFile(mode='w', suffix='.pem', delete=False) as tmp:
            tmp.write(pem)
            tmp_path = tmp.name
        subprocess.run([keytool, '-import', '-trustcacerts', '-noprompt',
                        '-alias', alias, '-file', tmp_path,
                        '-keystore', jdk24_cacerts, '-storepass', 'changeit'],
                       capture_output=True)
        os.unlink(tmp_path)
        print(f"CA imported into JDK 24 truststore: {alias}")
else:
    print("JDK 24 not yet installed; CA import skipped (foojay will download it on first build)")

# ── gradle.properties にプロキシ設定を書き込む ───────────────────────────────
jdk24_dir = os.path.expanduser('~/.jdks/jdk-24')
jdk24_home = jdk24_dir if os.path.isdir(jdk24_dir) else None

props = (
    f"systemProp.https.proxyHost={host}\n"
    f"systemProp.https.proxyPort={port}\n"
    f"systemProp.https.proxyUser={user}\n"
    f"systemProp.https.proxyPassword={password}\n"
    f"systemProp.http.proxyHost={host}\n"
    f"systemProp.http.proxyPort={port}\n"
    f"systemProp.http.proxyUser={user}\n"
    f"systemProp.http.proxyPassword={password}\n"
    f"systemProp.https.nonProxyHosts=localhost|127.0.0.1\n"
    f"systemProp.http.nonProxyHosts=localhost|127.0.0.1\n"
)
if jdk24_home:
    # デーモン用JVMとtoolchain検索パスの両方を設定する
    props += f"org.gradle.java.home={jdk24_home}\n"
    props += f"org.gradle.java.installations.paths={jdk24_home}\n"

with open(os.path.join(gradle_home, 'gradle.properties'), 'w') as f:
    f.write(props)
print(f"gradle.properties written (proxy={host}:{port}" + (f", jdk={jdk24_home}" if jdk24_home else "") + ")")

# ── Gradle distribution ───────────────────────────────────────────────────────
dist_url  = 'https://services.gradle.org/distributions/gradle-9.3.1-all.zip'
dist_name = 'gradle-9.3.1-all'
md5 = hashlib.md5(dist_url.encode()).digest()
n = int.from_bytes(md5, 'big')
chars = '0123456789abcdefghijklmnopqrstuvwxyz'
hash_str = ''
while n:
    hash_str = chars[n % 36] + hash_str
    n //= 36
dist_dir = os.path.join(gradle_home, 'wrapper', 'dists', dist_name, hash_str)
zip_path = os.path.join(dist_dir, f'{dist_name}.zip')

ok_marker = f"{zip_path}.ok"
if os.path.exists(ok_marker):
    print(f"Gradle distribution already ready: {dist_dir}")
    sys.exit(0)

if os.path.exists(zip_path):
    print(f"Gradle distribution zip already exists: {zip_path}")
    sys.exit(0)

os.makedirs(dist_dir, exist_ok=True)
for ext in ('lck', 'part'):
    p = f"{zip_path}.{ext}"
    if os.path.exists(p):
        os.remove(p)

download(dist_url, zip_path, opener)
print(f"Gradle distribution download complete: {zip_path}")
PYEOF
