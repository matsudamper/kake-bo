---
name: update-paparazzi-screenshots
description: Guide for managing Paparazzi screenshot tests. Use when adding new screen previews, updating existing screenshots, or understanding the screenshot testing setup.
---

# Paparazzi スクリーンショット管理ガイド

[Paparazzi](https://github.com/cashapp/paparazzi) と [ComposablePreviewScanner](https://github.com/sergio-sastre/ComposablePreviewScanner) を使用したスクリーンショットテストの管理手順。

---

## 仕組み

`@Preview` composable が自動的にスクリーンショットテストの対象になる。
`CommonComposablePreviewScanner` が `net.matsudamper.money.frontend.common.ui` パッケージ配下の `@Preview` アノテーション（`org.jetbrains.compose.ui.tooling.preview.Preview`）を付与した関数をスキャンし、Paparazzi でスナップショットを生成する。

### 関連ファイル

| ファイル | 役割 |
|---|---|
| `frontend/common/ui/src/androidUnitTest/kotlin/.../screenshot/ScreenshotTest.kt` | テストクラス（Parameterized + ComposablePreviewScanner） |
| `frontend/common/ui/src/test/snapshots/images/` | Paparazzi 参照画像の出力先 |
| `README/` | README.md 表示用の画像 |

---

## 新しい画面のスクリーンショットを追加する

### 1. @Preview composable を追加

対象の画面ファイル（`commonMain`）に `@Preview` 付き composable 関数を追加する。

```kotlin
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
private fun NewScreenPreview() {
    AppRoot {
        NewScreen(
            uiState = NewScreenUiState(
                // モックデータ
            ),
        )
    }
}
```

- `@Preview` は `org.jetbrains.compose.ui.tooling.preview.Preview` を使用する
- `private` で定義する
- `AppRoot` でラップしてテーマを適用する
- テストは自動的にこの関数を検出する（ScreenshotTest.kt の変更は不要）

### 2. スナップショットを生成

```shell
./gradlew :frontend:common:ui:recordPaparazziDebug
```

生成された画像は `frontend/common/ui/src/test/snapshots/images/` に出力される。

### 3. README 画像を更新（必要な場合）

`README/` フォルダに画像をコピーし、`README.md` の `## スクリーンショット` セクション内の `<table>` に `<td><img>` を追加する。

画像のファイル名は Paparazzi の出力（`frontend/common/ui/src/test/snapshots/images/`）から対応するものをコピーする。

---

## スナップショットの検証

既存の参照画像と比較してUIの差分を検出する。

```shell
./gradlew :frontend:common:ui:verifyPaparazziDebug
```

差分がある場合はテストが失敗する。意図した変更の場合は `recordPaparazziDebug` で参照画像を更新する。

---

## 既存の @Preview 関数一覧

| 画面 | ファイル | 関数名 |
|---|---|---|
| 期間分析（棒グラフ） | `screen/root/home/RootHomeTabPeriodAllContent.kt` | `PeriodAnalyticsScreenPreview` |
| ログイン | `screen/login/LoginScreen.kt` | `LoginScreenPreview` |
| 月次ホーム | `screen/root/home/monthly/RootHomeMonthlyScreen.kt` | `RootHomeMonthlyScreenPreview` |
| 支出追加 | `screen/addmoneyusage/AddMoneyUsageScreen.kt` | `AddMoneyUsageScreenPreview` |
| 設定 | `screen/root/settings/SettingScreen.kt` | `SettingScreenPreview` |
| カレンダー | `screen/root/usage/RootUsageCalendarScreen.kt` | `RootUsageCalendarScreenPreview`, `RootUsageCalendarScreenDarkPreview`, `RootUsageCalendarScreenLoadingPreview`, `RootUsageCalendarScreenWithManyItemsPreview`, `RootUsageCalendarScreenWithManyItemsDarkPreview` |

すべて `frontend/common/ui/src/commonMain/kotlin/net/matsudamper/money/frontend/common/ui/` 配下。
