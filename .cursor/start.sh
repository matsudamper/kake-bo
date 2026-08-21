#!/usr/bin/env bash
# 起動毎に実行される。注入された Secret を Gradle が参照できる形へ反映する。
# settings.gradle.kts は GitHub Packages の認証に gpr.user / gpr.key (または
# GITHUB_ACTOR / GITHUB_TOKEN 環境変数) を要求する。ここで Secret の
# GPR_USER / GPR_KEY を gpr.user / gpr.key へ入れ替える。
set -euo pipefail

GRADLE_HOME="${HOME}/.gradle"
GRADLE_PROPS="${GRADLE_HOME}/gradle.properties"
mkdir -p "${GRADLE_HOME}"
touch "${GRADLE_PROPS}"

if [ -z "${GPR_USER:-}" ] || [ -z "${GPR_KEY:-}" ]; then
  echo "WARNING: GPR_USER / GPR_KEY が未設定です。GitHub Packages の解決に失敗します。" >&2
  exit 0
fi

# 既存の gpr.* 行を除去してから最新の値を書き込む (冪等性のため)。
grep -v -E '^(gpr\.user|gpr\.key)=' "${GRADLE_PROPS}" > "${GRADLE_PROPS}.tmp" || true
mv "${GRADLE_PROPS}.tmp" "${GRADLE_PROPS}"
{
  echo "gpr.user=${GPR_USER}"
  echo "gpr.key=${GPR_KEY}"
} >> "${GRADLE_PROPS}"

echo "gpr.user / gpr.key を ${GRADLE_PROPS} に反映しました。"
