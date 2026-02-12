#!/bin/bash
set -euo pipefail

# Only run in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# ── Maven proxy ──────────────────────────────────────────────────────────────
echo "Configuring Maven proxy settings for Claude Code Web..."
mkdir -p ~/.m2

if [ -n "${HTTPS_PROXY:-}" ]; then
  proxy_no_scheme="${HTTPS_PROXY#http://}"
  proxy_no_scheme="${proxy_no_scheme#https://}"
  proxy_no_creds="${proxy_no_scheme##*@}"
  proxy_host="${proxy_no_creds%%:*}"
  proxy_port_with_path="${proxy_no_creds#*:}"
  proxy_port="${proxy_port_with_path%%/*}"

  no_proxy_hosts="${NO_PROXY:-localhost|127.0.0.1}"

  cat > ~/.m2/settings.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <proxies>
    <proxy>
      <id>http-proxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>${proxy_host}</host>
      <port>${proxy_port}</port>
      <nonProxyHosts>${no_proxy_hosts}</nonProxyHosts>
    </proxy>
    <proxy>
      <id>https-proxy</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>${proxy_host}</host>
      <port>${proxy_port}</port>
      <nonProxyHosts>${no_proxy_hosts}</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
EOF

  echo "Maven settings.xml generated: ${proxy_host}:${proxy_port}"
else
  echo "HTTPS_PROXY is not set; skipping Maven proxy configuration"
fi

# ── Gradle proxy + distribution download ─────────────────────────────────────
echo "Configuring Gradle proxy settings..."

python3 - <<'PYEOF'
import os, sys, hashlib, urllib.request, urllib.parse

proxy_url = os.environ.get('HTTPS_PROXY', '')
if not proxy_url:
    print("HTTPS_PROXY is not set; skipping Gradle proxy configuration")
    sys.exit(0)

parsed = urllib.parse.urlparse(proxy_url)
host     = parsed.hostname
port     = str(parsed.port)
user     = parsed.username or ''
password = parsed.password or ''

# ~/.gradle/gradle.properties にプロキシを書き込む
gradle_home = os.path.expanduser('~/.gradle')
os.makedirs(gradle_home, exist_ok=True)
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
with open(os.path.join(gradle_home, 'gradle.properties'), 'w') as f:
    f.write(props)
print(f"gradle.properties written: {host}:{port}")

# Gradle distribution のキャッシュパスを決定
dist_url = 'https://services.gradle.org/distributions/gradle-9.3.1-all.zip'
dist_name = 'gradle-9.3.1-all'
md5 = hashlib.md5(dist_url.encode()).digest()
n = int.from_bytes(md5, 'big')
chars = '0123456789abcdefghijklmnopqrstuvwxyz'
hash_str = ''
while n:
    hash_str = chars[n % 36] + hash_str
    n //= 36
dist_dir = os.path.join(gradle_home, 'wrapper', 'dists', dist_name, hash_str)
zip_path  = os.path.join(dist_dir, f'{dist_name}.zip')

# .ok マーカーがあれば展開済み → スキップ
ok_marker = f"{zip_path}.ok"
if os.path.exists(ok_marker):
    print(f"Gradle distribution already ready: {dist_dir}")
    sys.exit(0)

# ZIPが既にある場合はスキップ（wrapper が展開する）
if os.path.exists(zip_path):
    print(f"Gradle distribution zip already exists: {zip_path}")
    sys.exit(0)

os.makedirs(dist_dir, exist_ok=True)
# .lck / .part を削除してクリーンな状態にする
for ext in ('lck', 'part'):
    p = f"{zip_path}.{ext}"
    if os.path.exists(p):
        os.remove(p)

print(f"Downloading {dist_url} ...")
proxy_handler = urllib.request.ProxyHandler({'https': proxy_url, 'http': proxy_url})
opener = urllib.request.build_opener(proxy_handler)
with opener.open(dist_url) as resp:
    total = int(resp.headers.get('Content-Length', 0))
    downloaded = 0
    with open(zip_path, 'wb') as f:
        while True:
            chunk = resp.read(65536)
            if not chunk:
                break
            f.write(chunk)
            downloaded += len(chunk)
            if total:
                pct = downloaded * 100 // total
                print(f"\r  {pct}% ({downloaded}/{total} bytes)", end='', flush=True)
print(f"\nDownload complete: {zip_path}")
PYEOF
