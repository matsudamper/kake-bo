#!/bin/bash
set -euo pipefail

# Only run in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "Configuring Gradle proxy settings and downloading dependencies..."

python3 - <<'PYEOF'
import os, sys, hashlib, urllib.request, urllib.parse, zipfile, re, subprocess, tempfile, shutil

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

# ── プロキシ認証用 Java Agent ─────────────────────────────────────────────────
# sdkmanager 等の JVM ツールでプロキシ認証を有効にするための Java agent
# Gradle init スクリプトの Authenticator と同じパターン
agent_dir = os.path.join(gradle_home, 'proxy-auth-agent')
agent_jar = os.path.join(agent_dir, 'proxy-auth-agent.jar')

if not os.path.exists(agent_jar):
    os.makedirs(agent_dir, exist_ok=True)
    agent_java = os.path.join(agent_dir, 'ProxyAuthAgent.java')
    with open(agent_java, 'w') as f:
        f.write("""import java.lang.instrument.Instrumentation;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class ProxyAuthAgent {
    public static void premain(String args, Instrumentation inst) {
        String proxyUser = System.getProperty("https.proxyUser",
                           System.getProperty("http.proxyUser", ""));
        String proxyPass = System.getProperty("https.proxyPassword",
                           System.getProperty("http.proxyPassword", ""));
        if (!proxyUser.isEmpty() && !proxyPass.isEmpty()) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
                    }
                    return null;
                }
            });
        }
    }
}
""")
    manifest_path = os.path.join(agent_dir, 'MANIFEST.MF')
    with open(manifest_path, 'w') as f:
        f.write('Premain-Class: ProxyAuthAgent\n')
    java_home_tmp = os.environ.get('JAVA_HOME', '/usr/lib/jvm/java-21-openjdk-amd64')
    javac = os.path.join(java_home_tmp, 'bin', 'javac')
    jar_cmd = os.path.join(java_home_tmp, 'bin', 'jar')
    r1 = subprocess.run([javac, agent_java], cwd=agent_dir, capture_output=True, text=True)
    if r1.returncode == 0:
        r2 = subprocess.run([jar_cmd, 'cfm', agent_jar, 'MANIFEST.MF',
                            'ProxyAuthAgent.class', 'ProxyAuthAgent$1.class'],
                           cwd=agent_dir, capture_output=True, text=True)
        if r2.returncode == 0:
            print(f"Proxy auth Java agent created: {agent_jar}")
        else:
            print(f"Failed to create agent JAR: {r2.stderr.strip()}")
    else:
        print(f"Failed to compile ProxyAuthAgent: {r1.stderr.strip()}")

# ── Gradle デーモン JVM (JDK 21) の truststore にプロキシ CA を追加 ────────────
# HTTPS プロキシ (TLS 検査) の CA を JDK 21 truststore に追加する
# これがないと foojay resolver が api.foojay.io に接続できない

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

# ── gradle.properties にプロキシ設定を書き込む ─────────────────────────────────
def write_gradle_properties():
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
    with open(os.path.join(gradle_home, 'gradle.properties'), 'w') as f:
        f.write(props)
    print(f"gradle.properties written (proxy={host}:{port})")

# JDK 25 は gradle/gradle-daemon-jvm.properties と foojay-resolver により Gradle が自動で取得する
write_gradle_properties()

# ── Android SDK セットアップ ──────────────────────────────────────────────────
# sdkmanager のセットアップのみ（パッケージはビルド時に AGP が自動ダウンロード）

project_dir = os.environ.get('CLAUDE_PROJECT_DIR', os.getcwd())
android_home = os.path.expanduser('~/android-sdk')
local_props = os.path.join(project_dir, 'local.properties')

os.makedirs(android_home, exist_ok=True)

# cmdline-tools のダウンロード（sdkmanager 本体の取得）
cmdline_tools_dir = os.path.join(android_home, 'cmdline-tools', 'latest')
sdkmanager_bin = os.path.join(cmdline_tools_dir, 'bin', 'sdkmanager')
if not os.path.exists(sdkmanager_bin):
    cmdline_url = 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip'
    cmdline_zip = os.path.join(android_home, 'commandlinetools.zip')
    # プロキシ経由だと転送が途中で切れて壊れた zip になることがあるため、検証してリトライする
    for attempt in range(1, 5):
        if os.path.exists(cmdline_zip):
            os.unlink(cmdline_zip)
        try:
            download(cmdline_url, cmdline_zip, opener)
        except Exception as e:
            print(f"cmdline-tools download failed (attempt {attempt}): {e}")
            continue
        if zipfile.is_zipfile(cmdline_zip):
            break
        print(f"cmdline-tools archive is corrupted (attempt {attempt})")
    else:
        raise RuntimeError('cmdline-tools could not be downloaded')
    with zipfile.ZipFile(cmdline_zip, 'r') as zf:
        zf.extractall(android_home)
    os.unlink(cmdline_zip)
    # zip は cmdline-tools/ に展開される → cmdline-tools/latest/ に移動
    extracted = os.path.join(android_home, 'cmdline-tools')
    temp_dir = os.path.join(android_home, '_cmdline-tools-temp')
    os.rename(extracted, temp_dir)
    os.makedirs(extracted, exist_ok=True)
    shutil.move(temp_dir, cmdline_tools_dir)
    os.chmod(sdkmanager_bin, 0o755)
    print(f"cmdline-tools installed: {cmdline_tools_dir}")

# ライセンス承認
licenses_dir = os.path.join(android_home, 'licenses')
os.makedirs(licenses_dir, exist_ok=True)
with open(os.path.join(licenses_dir, 'android-sdk-license'), 'w') as f:
    f.write('\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n')

# local.properties に sdk.dir を書き込む
with open(local_props, 'w') as f:
    f.write(f"sdk.dir={android_home}\n")
print(f"local.properties written: sdk.dir={android_home}")

# ── GraalVM native-image のセットアップ ──────────────────────────────────────
# /opt/graalvm* にインストール済みの GraalVM を探し、native-image を PATH に追加する
import glob as _glob

graalvm_dirs = sorted(_glob.glob('/opt/graalvm*'), reverse=True)
graalvm_home = next((d for d in graalvm_dirs if os.path.isfile(os.path.join(d, 'bin', 'native-image'))), None)

if graalvm_home:
    native_image_bin = os.path.join(graalvm_home, 'bin', 'native-image')
    symlink_path = '/usr/local/bin/native-image'
    if not os.path.exists(symlink_path):
        try:
            os.symlink(native_image_bin, symlink_path)
            print(f"GraalVM native-image symlinked: {symlink_path} -> {native_image_bin}")
        except OSError as e:
            print(f"Failed to symlink native-image: {e}")
    else:
        print(f"GraalVM native-image already available: {symlink_path}")
    # GRAALVM_HOME を環境変数ファイルに書き込み、セッション全体で参照できるようにする
    env_file = os.environ.get('CLAUDE_ENV_FILE', '')
    if env_file:
        with open(env_file, 'a') as f:
            f.write(f'export GRAALVM_HOME="{graalvm_home}"\n')
            f.write(f'export PATH="{graalvm_home}/bin:$PATH"\n')
    print(f"GraalVM home: {graalvm_home}")
else:
    print("WARNING: GraalVM not found in /opt/graalvm*. native-image will not be available.")

print("Session start hook completed.")
PYEOF
