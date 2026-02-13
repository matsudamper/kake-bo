#!/bin/bash
set -euo pipefail

# Only run in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "Configuring Gradle proxy settings and downloading dependencies..."

python3 - <<'PYEOF'
import os, sys, hashlib, urllib.request, urllib.parse, zipfile

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

# ── Gradle デーモン JVM (JDK 21) の truststore にプロキシ CA を追加 ────────────
# HTTPS プロキシ (TLS 検査) の CA を JDK 21 truststore に追加する
# これがないと foojay resolver が api.foojay.io に接続できない
import re, subprocess, tempfile

java_home = os.environ.get('JAVA_HOME', '/usr/lib/jvm/java-21-openjdk-amd64')

def import_ca_into_jdk(jdk_path, label):
    """指定した JDK の truststore にプロキシ CA をインポートする"""
    cacerts = os.path.join(jdk_path, 'lib', 'security', 'cacerts')
    keytool = os.path.join(jdk_path, 'bin', 'keytool')
    cacerts_real = os.path.realpath(cacerts)
    sys_ca_bundle = '/etc/ssl/certs/ca-certificates.crt'
    if not (os.path.exists(sys_ca_bundle) and os.path.exists(keytool)):
        return
    with open(sys_ca_bundle) as f:
        bundle = f.read()
    pem_blocks = re.findall(r'-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----', bundle, re.DOTALL)
    for pem in pem_blocks:
        result = subprocess.run(['openssl', 'x509', '-noout', '-subject'], input=pem, capture_output=True, text=True)
        if 'Anthropic' not in result.stdout:
            continue
        cn_match = re.search(r'CN\s*=\s*([^\n,]+)', result.stdout)
        alias = cn_match.group(1).strip().lower().replace(' ', '-') if cn_match else 'anthropic-ca'
        check = subprocess.run([keytool, '-list', '-alias', alias, '-keystore', cacerts_real, '-storepass', 'changeit'],
                               capture_output=True, text=True)
        if check.returncode == 0:
            print(f"CA already imported into {label}: {alias}")
            continue
        with tempfile.NamedTemporaryFile(mode='w', suffix='.pem', delete=False) as tmp:
            tmp.write(pem)
            tmp_path = tmp.name
        r = subprocess.run([keytool, '-import', '-trustcacerts', '-noprompt',
                        '-alias', alias, '-file', tmp_path,
                        '-keystore', cacerts_real, '-storepass', 'changeit'],
                       capture_output=True, text=True)
        os.unlink(tmp_path)
        if r.returncode == 0:
            print(f"CA imported into {label} truststore: {alias}")
        else:
            print(f"Failed to import CA into {label}: {alias} ({r.stderr.strip()})")

def enable_basic_auth_tunneling(jdk_path, label):
    """JDK の net.properties で HTTPS トンネリング時の Basic 認証を有効化する"""
    net_props = os.path.join(jdk_path, 'conf', 'net.properties')
    if not os.path.exists(net_props):
        return
    with open(net_props) as f:
        content = f.read()
    if 'jdk.http.auth.tunneling.disabledSchemes=Basic' in content:
        content = content.replace(
            'jdk.http.auth.tunneling.disabledSchemes=Basic',
            'jdk.http.auth.tunneling.disabledSchemes='
        )
        with open(net_props, 'w') as f:
            f.write(content)
        print(f"Enabled Basic auth for HTTPS proxy tunneling in {label} net.properties")

# JDK 21 のセットアップ
import_ca_into_jdk(java_home, 'JDK 21')
enable_basic_auth_tunneling(java_home, 'JDK 21')

# ── gradle.properties にプロキシ設定を書き込む（初期版：JDK 21 で起動）──────────
def write_gradle_properties(jdk_home=None):
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
        f"systemProp.jdk.http.auth.tunneling.disabledSchemes=\n"
    )
    if jdk_home:
        props += f"org.gradle.java.home={jdk_home}\n"
        props += f"org.gradle.java.installations.paths={jdk_home}\n"
    with open(os.path.join(gradle_home, 'gradle.properties'), 'w') as f:
        f.write(props)
    print(f"gradle.properties written (proxy={host}:{port}" + (f", jdk={jdk_home}" if jdk_home else "") + ")")

# ── foojay がダウンロードした JDK 24 を探す ───────────────────────────────────
def find_jdk24_home():
    """foojay がダウンロードした JDK 24 のホームディレクトリを探す"""
    search_dirs = [
        os.path.expanduser('~/.gradle/jdks'),
        os.path.expanduser('~/.jdks/jdk-24'),
    ]
    for d in search_dirs:
        if not os.path.isdir(d):
            continue
        keytool = os.path.join(d, 'bin', 'keytool')
        if os.path.exists(keytool):
            return d
        for entry in os.listdir(d):
            sub = os.path.join(d, entry)
            keytool = os.path.join(sub, 'bin', 'keytool')
            if os.path.exists(keytool):
                return sub
    return None

jdk24_home = find_jdk24_home()

if jdk24_home:
    # JDK 24 が既にある場合はセットアップして完了
    import_ca_into_jdk(jdk24_home, 'JDK 24')
    enable_basic_auth_tunneling(jdk24_home, 'JDK 24')
    write_gradle_properties(jdk_home=jdk24_home)
else:
    # JDK 24 がまだない場合：まず JDK 21 で gradle.properties を書き込み、
    # foojay にダウンロードさせるために Gradle をプライミング実行する
    write_gradle_properties()

    # Gradle distribution の事前ダウンロード
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
    if not os.path.exists(ok_marker) and not os.path.exists(zip_path):
        os.makedirs(dist_dir, exist_ok=True)
        for ext in ('lck', 'part'):
            p = f"{zip_path}.{ext}"
            if os.path.exists(p):
                os.remove(p)
        download(dist_url, zip_path, opener)
        print(f"Gradle distribution download complete: {zip_path}")

    # foojay プライミング: Gradle を実行して JDK 24 のダウンロードをトリガーする
    # build-logic の構成時に foojay が JDK 24 をダウンロードする
    # ビルド自体は JDK 21 デーモンでは失敗するが、JDK 24 はダウンロードされる
    project_dir = os.environ.get('CLAUDE_PROJECT_DIR', os.getcwd())
    gradlew = os.path.join(project_dir, 'gradlew')
    print("Priming Gradle to trigger foojay JDK 24 download...")
    subprocess.run([gradlew, 'help'], capture_output=True, text=True, cwd=project_dir)

    # ダウンロードされた JDK 24 を探す
    jdk24_home = find_jdk24_home()
    if jdk24_home:
        print(f"JDK 24 downloaded by foojay: {jdk24_home}")
        import_ca_into_jdk(jdk24_home, 'JDK 24')
        enable_basic_auth_tunneling(jdk24_home, 'JDK 24')
        write_gradle_properties(jdk_home=jdk24_home)

        # 古いデーモン (JDK 21) を停止
        subprocess.run([gradlew, '--stop'], capture_output=True, text=True, cwd=project_dir)
        print("Stopped old Gradle daemon (JDK 21)")
    else:
        print("WARNING: JDK 24 was not downloaded by foojay. Build may fail.")

# ── Android SDK セットアップ ──────────────────────────────────────────────────
# sdkmanager はプロキシ認証に対応していないため、Python で直接ダウンロードする
import shutil

project_dir = os.environ.get('CLAUDE_PROJECT_DIR', os.getcwd())
android_home = os.path.expanduser('~/android-sdk')
local_props = os.path.join(project_dir, 'local.properties')

# platforms/android-36 が既にインストール済みかチェック
platform_dir = os.path.join(android_home, 'platforms', 'android-36')
if os.path.isdir(platform_dir):
    print(f"Android SDK already installed: {platform_dir}")
else:
    base_url = 'https://dl.google.com/android/repository/'
    # パッケージ定義: (zip名, 展開時のトップディレクトリ名, 配置先)
    sdk_packages = [
        ('platform-36_r02.zip', 'android-36', 'platforms/android-36'),
        ('build-tools_r36_linux.zip', 'android-16', 'build-tools/36.0.0'),
        ('platform-tools_r36.0.2-linux.zip', 'platform-tools', 'platform-tools'),
    ]
    os.makedirs(android_home, exist_ok=True)

    for zip_name, extracted_top, target_dir in sdk_packages:
        dest = os.path.join(android_home, target_dir)
        if os.path.isdir(dest):
            print(f"Already installed: {target_dir}")
            continue
        url = base_url + zip_name
        zip_path = os.path.join(android_home, zip_name)
        download(url, zip_path, opener)
        with zipfile.ZipFile(zip_path, 'r') as zf:
            zf.extractall(android_home)
        os.unlink(zip_path)
        extracted = os.path.join(android_home, extracted_top)
        if os.path.isdir(extracted) and not os.path.isdir(dest):
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            shutil.move(extracted, dest)
        print(f"Installed: {target_dir}")

# ライセンスファイル作成
licenses_dir = os.path.join(android_home, 'licenses')
os.makedirs(licenses_dir, exist_ok=True)
with open(os.path.join(licenses_dir, 'android-sdk-license'), 'w') as f:
    f.write('\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n')

# local.properties に sdk.dir を書き込む
if os.path.isdir(android_home):
    with open(local_props, 'w') as f:
        f.write(f"sdk.dir={android_home}\n")
    print(f"local.properties written: sdk.dir={android_home}")

print("Session start hook completed.")
PYEOF
