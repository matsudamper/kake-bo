#!/bin/bash
set -euo pipefail

# Only run in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
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
