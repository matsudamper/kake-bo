#!/usr/bin/env bash
set -euo pipefail

CMDLINE_TOOLS_REVISION="16111833"
# AGP の DEFAULT_BUILD_TOOLS_REVISION。compileSdk とは独立していて、
# 揃えないと AGP がビルド中に別バージョンを取りに行く
BUILD_TOOLS_VERSION="36.0.0"
# compileSdk と別バージョンの platform を入れても AGP が SDK 未検出で落ちるので、ビルドファイルを正とする
COMPILE_SDK_FILE="frontend/android/app/build.gradle.kts"

if [ -n "${CLAUDE_PROJECT_DIR:-}" ] && [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# セットアップスクリプト欄から呼ぶときは cwd がリポジトリ外なので、自分の位置から解決する
cd "${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"

# 構築済みの SDK を無視して入れ直さないよう、local.properties の sdk.dir も候補にする
sdk_dir_in_local_properties=""
if [ -f local.properties ]; then
  sdk_dir_in_local_properties="$(sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*\(.*\)$/\1/p' local.properties | tail -n 1)"
fi
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-${sdk_dir_in_local_properties:-${HOME}/android-sdk}}}"
SDKMANAGER="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"
ANDROID_CLI="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/android"

compile_sdk_major="$(sed -n -E \
  -e 's/^[[:space:]]*(androidCompileSdk|compileSdk)[[:space:]]*=[[:space:]]*"?([0-9]+)"?[[:space:]]*$/\2/p' \
  -e 's/.*compileSdkVersion\("android-([0-9]+)\.[0-9]+"\).*/\1/p' \
  "${COMPILE_SDK_FILE}" | head -n 1)"
compile_sdk_minor="$(sed -n -E \
  -e 's/^[[:space:]]*(androidCompileSdkMinor|compileSdkMinor)[[:space:]]*=[[:space:]]*"?([0-9]+)"?[[:space:]]*$/\2/p' \
  -e 's/.*compileSdkVersion\("android-[0-9]+\.([0-9]+)"\).*/\1/p' \
  "${COMPILE_SDK_FILE}" | head -n 1)"
if [ -z "${compile_sdk_major}" ]; then
  echo "${COMPILE_SDK_FILE} から compileSdk を読めなかった" >&2
  exit 1
fi
compile_sdk_minor="${compile_sdk_minor:-0}"
# SDK のパッケージ名は API 36 までは android-36 / android-36.1、API 37 からは android-37.0 と .0 も付く
if [ "${compile_sdk_major}" -le 36 ] && [ "${compile_sdk_minor}" = 0 ]; then
  platform="android-${compile_sdk_major}"
else
  platform="android-${compile_sdk_major}.${compile_sdk_minor}"
fi

# SessionStart フックからも呼ぶので、揃っているときは sdkmanager を起動しない。
# sdkmanager は何もすることが無くてもリモートのリポジトリを引きに行って数秒かかる
if [ ! -f "${ANDROID_SDK_ROOT}/platforms/${platform}/android.jar" ] ||
  [ ! -d "${ANDROID_SDK_ROOT}/build-tools/${BUILD_TOOLS_VERSION}" ] ||
  [ ! -x "${ANDROID_SDK_ROOT}/platform-tools/adb" ] ||
  [ ! -x "${ANDROID_CLI}" ]; then
  # これまで入れていた 13114758 の cmdline-tools には Android CLI(android コマンド)が無いので、android の有無で入れ替える
  if [ ! -x "${ANDROID_CLI}" ]; then
    echo "[setup] cmdline-tools を導入する"
    mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"
    tmp="$(mktemp -d)"
    trap 'rm -rf "${tmp}"' EXIT
    # プロキシ経由だと転送が途中で切れて壊れた zip になることがあるので、確かめて取り直す
    for attempt in 1 2 3 4; do
      if curl -fsSL -o "${tmp}/cmdline-tools.zip" "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_REVISION}_latest.zip" &&
        unzip -tq "${tmp}/cmdline-tools.zip" > /dev/null; then
        break
      fi
      if [ "${attempt}" = 4 ]; then
        echo "cmdline-tools を取得できなかった" >&2
        exit 1
      fi
    done
    unzip -q "${tmp}/cmdline-tools.zip" -d "${tmp}"
    rm -rf "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
    mv "${tmp}/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
  fi

  echo "[setup] Android SDK パッケージを導入する (${platform})"
  # yes だと SIGPIPE で 141 を返し pipefail に引っかかるので、有限個の y を流す
  { for _ in $(seq 1 200); do printf 'y\n'; done; } | "${SDKMANAGER}" --sdk_root="${ANDROID_SDK_ROOT}" --licenses > /dev/null
  "${SDKMANAGER}" --sdk_root="${ANDROID_SDK_ROOT}" --install \
    "platform-tools" \
    "platforms;${platform}" \
    "build-tools;${BUILD_TOOLS_VERSION}" > /dev/null
fi

# 環境変数(ANDROID_HOME)はセットアップ後のシェルに残らないので local.properties に書く。
# cmake.dir など他のローカル設定を消さないよう sdk.dir の行だけ差し替える
tmp_local_properties="$(mktemp ./.local.properties.XXXXXX)"
if [ -f local.properties ]; then
  grep -v -E '^[[:space:]]*sdk\.dir[[:space:]]*=' local.properties > "${tmp_local_properties}" || true
fi
echo "sdk.dir=${ANDROID_SDK_ROOT}" >> "${tmp_local_properties}"
mv "${tmp_local_properties}" local.properties

# hook の環境変数は CLAUDE_ENV_FILE に書いたものだけがセッションに残る
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export PATH=\"${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:\$PATH\"" >> "${CLAUDE_ENV_FILE}"
fi

android_user_home="${ANDROID_USER_HOME:-${HOME}/.android}"
if [ ! -f "${android_user_home}/debug.keystore" ]; then
  echo "[setup] debug.keystore を作る"
  mkdir -p "${android_user_home}"
  keytool -genkeypair -keystore "${android_user_home}/debug.keystore" \
    -storepass android -alias androiddebugkey -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" > /dev/null
fi

# ── GraalVM native-image ──
# /opt の GraalVM は PATH に入っていないので、native-image を持つ中で名前が最も新しいものを通す
graalvm_home=""
for dir in $(ls -d /opt/graalvm* 2>/dev/null | sort -r); do
  if [ -x "${dir}/bin/native-image" ]; then
    graalvm_home="${dir}"
    break
  fi
done
if [ -n "${graalvm_home}" ]; then
  if [ ! -e /usr/local/bin/native-image ]; then
    ln -s "${graalvm_home}/bin/native-image" /usr/local/bin/native-image ||
      echo "[setup] native-image のリンクを作れなかった" >&2
  fi
  if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    {
      echo "export GRAALVM_HOME=\"${graalvm_home}\""
      echo "export PATH=\"${graalvm_home}/bin:\$PATH\""
    } >> "${CLAUDE_ENV_FILE}"
  fi
else
  echo "[setup] /opt/graalvm* に GraalVM が無いので native-image は使えない" >&2
fi

# ── Kotlin/Wasm の npm ツールが使う karma ──
# Kotlin プラグインは karma を github:Kotlin/karma から取るが、このセッションの GitHub プロキシは
# codeload.github.com の tarball を拒否する。git fetch は通るので tarball を作り yarn の offline mirror に置く
# Kotlin プラグインを更新して karma のコミットが変わったら KARMA_SHA も更新する
KARMA_SHA=239a8fc984584f0d96b1dd750e7a5e2c79da93a6
YARN_OFFLINE_MIRROR="$HOME/.cache/yarn-offline-mirror"
mkdir -p "$YARN_OFFLINE_MIRROR"
if [ ! -f "$YARN_OFFLINE_MIRROR/$KARMA_SHA" ]; then
  karma_tmp=$(mktemp -d)
  # 書き込み途中で中断されても完成品と誤認しないよう、一時ファイルに書いてから置き換える
  karma_partial="$YARN_OFFLINE_MIRROR/$KARMA_SHA.partial"
  if git -C "$karma_tmp" init -q \
    && git -C "$karma_tmp" fetch -q --depth 1 https://github.com/Kotlin/karma "$KARMA_SHA" \
    && git -C "$karma_tmp" archive --format=tar.gz --prefix=package/ -o "$karma_partial" FETCH_HEAD \
    && mv "$karma_partial" "$YARN_OFFLINE_MIRROR/$KARMA_SHA"; then
    echo "[setup] karma の tarball を yarn offline mirror に置いた: $YARN_OFFLINE_MIRROR/$KARMA_SHA"
  else
    rm -f "$karma_partial"
    echo "[setup] karma の tarball を用意できなかった。Kotlin/Wasm の npm install が失敗する可能性がある" >&2
  fi
  rm -rf "$karma_tmp"
fi
printf 'yarn-offline-mirror "%s"\nyarn-offline-mirror-pruning false\n' "$YARN_OFFLINE_MIRROR" > "$HOME/.yarnrc"

echo "[setup] 完了"
