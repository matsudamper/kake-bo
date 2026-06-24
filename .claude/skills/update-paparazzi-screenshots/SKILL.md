---
name: update-paparazzi-screenshots
description: Guide for updating existing README screenshot images and keeping them in sync with current UI.
---

# README.mdのスクリーンショットを更新する

このスキルは、README に掲載しているスクリーンショットを更新するための手順を示す。

## 仕組み
[Paparazzi](https://github.com/cashapp/paparazzi) と [ComposablePreviewScanner](https://github.com/sergio-sastre/ComposablePreviewScanner) を使用している。

`@Preview` composable が自動的にスクリーンショットテストの対象になる。
`AndroidComposablePreviewScanner` が `net.matsudamper.money.frontend.common.ui` パッケージ配下の `@Preview` アノテーション（`androidx.compose.ui.tooling.preview.Preview`）を付与した関数をスキャンし、Paparazzi でスナップショットを生成する。

### 関連ファイル

| ファイル | 役割 |
|---|---|
| `frontend/common/ui/src/androidUnitTest/kotlin/.../screenshot/ScreenshotTest.kt` | テストクラス（Parameterized + ComposablePreviewScanner） |
| `frontend/common/ui/src/test/snapshots/images/` | Paparazzi 参照画像の出力先 |
| `README/` | README.md 表示用の画像 |

---

## 既存画面のスクリーンショットを更新する

### 1. スナップショットを生成

```shell
./gradlew :frontend:common:ui:recordPaparazziDebug
```

生成された画像は `frontend/common/ui/src/test/snapshots/images/` に出力される。

### 2. README 画像を同期

- README.mdの先頭のtable名にある画像ファイル名と内容から推測して対応する画像に置き換える
