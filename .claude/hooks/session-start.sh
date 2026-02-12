#!/bin/bash
set -euo pipefail

# Only run in Claude Code Web remote environment
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "Configuring Maven proxy settings for Claude Code Web..."
mkdir -p ~/.m2

if [ -n "${HTTPS_PROXY:-}" ]; then
  # Strip scheme and optional credentials, then split host/port.
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
