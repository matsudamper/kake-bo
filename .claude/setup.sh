#!/usr/bin/env bash
set -euo pipefail

cd "$(cd "$(dirname "$0")/.." && pwd)"

sdk_dir_in_local_properties=""
if [ -f local.properties ]; then
  sdk_dir_in_local_properties="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*\(.*\)$/\1/p' local.properties | tail -n 1)"
fi
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-${sdk_dir_in_local_properties:-${HOME}/android-sdk}}}"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"
BUILD_TOOLS_VERSION="36.0.0"
SDKMANAGER="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"

compile_sdk="$(sed -n 's/^[[:space:]]*compileSdk = \(.*\)$/\1/p' frontend/android/app/build.gradle.kts | head -n 1)"
if [ -z "${compile_sdk}" ]; then
  echo "frontend/android/app/build.gradle.kts から compileSdk を読めなかった" >&2
  exit 1
fi

if [ ! -f "${ANDROID_SDK_ROOT}/platforms/android-${compile_sdk}/android.jar" ] ||
  [ ! -d "${ANDROID_SDK_ROOT}/build-tools/${BUILD_TOOLS_VERSION}" ] ||
  [ ! -x "${ANDROID_SDK_ROOT}/platform-tools/adb" ]; then
  if [ ! -x "${SDKMANAGER}" ]; then
    echo "[setup] cmdline-tools を導入する"
    mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"
    tmp="$(mktemp -d)"
    trap 'rm -rf "${tmp}"' EXIT
    curl -fsSL -o "${tmp}/cmdline-tools.zip" "${CMDLINE_TOOLS_URL}"
    unzip -q "${tmp}/cmdline-tools.zip" -d "${tmp}"
    rm -rf "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
    mv "${tmp}/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
  fi

  echo "[setup] Android SDK パッケージを導入する (compileSdk=${compile_sdk})"
  { for _ in $(seq 1 200); do printf 'y\n'; done; } | "${SDKMANAGER}" --sdk_root="${ANDROID_SDK_ROOT}" --licenses > /dev/null
  "${SDKMANAGER}" --sdk_root="${ANDROID_SDK_ROOT}" --install \
    "platform-tools" \
    "platforms;android-${compile_sdk}" \
    "build-tools;${BUILD_TOOLS_VERSION}" > /dev/null
fi

tmp_local_properties="$(mktemp ./.local.properties.XXXXXX)"
if [ -f local.properties ]; then
  grep -v -E '^[[:space:]]*sdk\.dir[[:space:]]*=' local.properties > "${tmp_local_properties}" || true
fi
echo "sdk.dir=${ANDROID_SDK_ROOT}" >> "${tmp_local_properties}"
mv "${tmp_local_properties}" local.properties

android_user_home="${ANDROID_USER_HOME:-${HOME}/.android}"
if [ ! -f "${android_user_home}/debug.keystore" ]; then
  echo "[setup] debug.keystore を作る"
  mkdir -p "${android_user_home}"
  keytool -genkeypair -keystore "${android_user_home}/debug.keystore" \
    -storepass android -alias androiddebugkey -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" > /dev/null
  chmod 600 "${android_user_home}/debug.keystore"
fi

gradle_user_home="${GRADLE_USER_HOME:-${HOME}/.gradle}"
gradle_properties="${gradle_user_home}/gradle.properties"

read_property() {
  [ -f "${gradle_properties}" ] || return 0
  sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*\\(.*\\)$/\\1/p" "${gradle_properties}" | tail -n 1
}

gpr_user="${GPR_USER:-${GITHUB_ACTOR:-$(read_property 'gpr\.user')}}"
gpr_key="${GPR_KEY:-${GITHUB_TOKEN:-$(read_property 'gpr\.key')}}"

describe_source() {
  for name in "$@"; do
    eval "value=\${${name}:-}"
    [ -n "${value}" ] && { echo "${name}"; return 0; }
  done
  echo "無し"
}

if [ -z "${gpr_user}" ] || [ -z "${gpr_key}" ]; then
  cat >&2 <<MSG
GitHub Packages(read:packages)の資格情報が揃っていない。無いと Gradle は構成段階で落ちる。
  gpr.user: $(describe_source GPR_USER GITHUB_ACTOR) / ${gradle_properties}: $([ -n "$(read_property 'gpr\.user')" ] && echo 有り || echo 無し)
  gpr.key : $(describe_source GPR_KEY GITHUB_TOKEN) / ${gradle_properties}: $([ -n "$(read_property 'gpr\.key')" ] && echo 有り || echo 無し)
MSG
  exit 1
fi

mkdir -p "${gradle_user_home}"
tmp_properties="$(mktemp "${gradle_user_home}/.gradle.properties.XXXXXX")"
chmod 600 "${tmp_properties}"
if [ -f "${gradle_properties}" ]; then
  grep -v -E '^[[:space:]]*gpr\.(user|key)[[:space:]]*=' "${gradle_properties}" > "${tmp_properties}" || true
fi
{
  echo "gpr.user=${gpr_user}"
  echo "gpr.key=${gpr_key}"
} >> "${tmp_properties}"
mv "${tmp_properties}" "${gradle_properties}"

echo "[setup] 完了"
