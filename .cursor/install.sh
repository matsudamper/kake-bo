#!/usr/bin/env bash
# Cloud Agent 環境のツールチェーンをセットアップする汎用スクリプト。
# アカウント固有の認証情報 (GitHub Packages の資格情報など) はここでは扱わない。
# それらは環境設定 (Web UI) 側の install/start コマンドで注入する。
set -euo pipefail

JDK_VERSION=24
JDK_DIR="${HOME}/jdks"
ANDROID_HOME="${HOME}/android-sdk"
GRADLE_HOME="${HOME}/.gradle"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> JDK ${JDK_VERSION} をセットアップ"
mkdir -p "${JDK_DIR}"
JDK_HOME="$(find "${JDK_DIR}" -maxdepth 1 -type d -name "jdk-${JDK_VERSION}*" | head -1 || true)"
if [ -z "${JDK_HOME}" ] || [ ! -x "${JDK_HOME}/bin/java" ]; then
  echo "    Temurin JDK ${JDK_VERSION} をダウンロード"
  curl -fsSL -o "${JDK_DIR}/jdk.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/ga/linux/x64/jdk/hotspot/normal/eclipse"
  tar -xzf "${JDK_DIR}/jdk.tar.gz" -C "${JDK_DIR}"
  rm -f "${JDK_DIR}/jdk.tar.gz"
  JDK_HOME="$(find "${JDK_DIR}" -maxdepth 1 -type d -name "jdk-${JDK_VERSION}*" | head -1)"
fi
echo "    JDK_HOME=${JDK_HOME}"
"${JDK_HOME}/bin/java" -version

echo "==> Gradle デーモンの JDK を設定"
# build-logic が JVM 24 をターゲットにするため、Gradle デーモン自体が JDK 24 で起動する必要がある。
mkdir -p "${GRADLE_HOME}"
GRADLE_PROPS="${GRADLE_HOME}/gradle.properties"
touch "${GRADLE_PROPS}"
# 既存の JDK 設定行を除去してから追記する (冪等性のため)。
grep -v -E '^(org\.gradle\.java\.home|org\.gradle\.java\.installations\.paths)=' "${GRADLE_PROPS}" > "${GRADLE_PROPS}.tmp" || true
mv "${GRADLE_PROPS}.tmp" "${GRADLE_PROPS}"
{
  echo "org.gradle.java.home=${JDK_HOME}"
  echo "org.gradle.java.installations.paths=${JDK_HOME}"
} >> "${GRADLE_PROPS}"

echo "==> Android SDK をセットアップ"
mkdir -p "${ANDROID_HOME}"
CMDLINE_TOOLS="${ANDROID_HOME}/cmdline-tools/latest"
if [ ! -x "${CMDLINE_TOOLS}/bin/sdkmanager" ]; then
  echo "    cmdline-tools をダウンロード"
  curl -fsSL -o "${ANDROID_HOME}/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  rm -rf "${ANDROID_HOME}/cmdline-tools"
  unzip -q "${ANDROID_HOME}/cmdline-tools.zip" -d "${ANDROID_HOME}"
  rm -f "${ANDROID_HOME}/cmdline-tools.zip"
  mkdir -p "${CMDLINE_TOOLS}"
  # zip は cmdline-tools/ 直下に展開されるため latest/ に移動する。
  mv "${ANDROID_HOME}/cmdline-tools/"* "${CMDLINE_TOOLS}/" 2>/dev/null || true
fi

SDKMANAGER="${CMDLINE_TOOLS}/bin/sdkmanager"
echo "    ライセンスに同意"
yes | "${SDKMANAGER}" --licenses --sdk_root="${ANDROID_HOME}" >/dev/null 2>&1 || true
echo "    必要な SDK パッケージをインストール"
"${SDKMANAGER}" --sdk_root="${ANDROID_HOME}" \
  "platform-tools" "platforms;android-36" "build-tools;36.0.0"

echo "==> local.properties を書き込み"
echo "sdk.dir=${ANDROID_HOME}" > "${REPO_ROOT}/local.properties"

echo "==> install 完了"
