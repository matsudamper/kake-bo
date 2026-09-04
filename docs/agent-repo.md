# kake-bo 固有ルール

## 概要
メールをパースして登録する家計簿。決済メール中心。

## Tech / 構成
- Backend: Ktor, GraphQL, MariaDB, Lettuce, FIDO(WebAuthn4J)
- Frontend: Kotlin/JS, Compose Multiplatform (WASM + Android), Apollo, Compose HTML
- 詳細なモジュール構成はリポジトリ構成を参照
- コーディングスタイル詳細: @docs/coding_style.md

## 実装注意
- コマンド引数のpathは相対
- メール・通知パーサのテストは書かない。例コメントはマスキング/ダミーのみ。ユーザー依頼文の生掲載禁止
- `material-icons-extended` 禁止。svg(xml)のみ。`frontend.common.ui` に配置
- プロジェクト内で絶対path禁止。相対path

## フロントエンド
- MVVM
- Composable内で早期returnしない（if-else / when）
- UiStateがUIとの接点。ViewModel関数をUIから直接呼ばない
- ViewModelStateに情報を詰め、それをデータソースにUiStateを作る。combine多用は兆候
- UIイベントはUiState内のイベントハンドラ（`@Immutable interface`）。Lambda直置きしない
- 表示に不要な値はUiStateに入れない
- Apollo Pagingは `updateOperation()` で最初のOperationに連結し `watch()` する

## ビルド
- モジュール個別ビルド禁止。Gradle並列禁止
```sh
./gradlew :backend:assemble :frontend:app:jsBrowserDevelopmentWebpack :frontend:android:app:assembleDebug --quiet
./gradlew ktlintFormat
./gradlew allTests --quiet
```
- Android依存なし: Kotest (JUnit 5) / Android・Robolectric: JUnit 4
- GraphQL schema: skill `@.claude/skills/download-graphql-schema`、`./gradlew generateApolloSources`
