# プロジェクトについて

メールをパースして登録する家計簿アプリ。決済メールを中心とした家計簿ソフト。

# Tech Stack

## Backend

- Ktor server framework
- GraphQL (graphql-java, GraphQL Java Kickstart)
- MariaDB for persistence
- Lettuce for sessions/challenges
- FIDO for authentication(WebAuthn4J)

## Frontend

- Kotlin/JS for web
- Jetpack Compose Multiplatform (WASM + Android)
- Apollo Client for GraphQL
- Compose HTML

# Architecture

## Module Structure

The project is organized into backend, frontend, and shared modules:

- `backend/`: Ktor server application
    - `app/`: Main application and GraphQL data loaders
    - `app/interfaces/`: Repository interfaces
    - `base/`: Core utilities and ServerEnv configuration
    - `di/`: Dependency injection container (MainDiContainer)
    - `datasource/`: Data layer (DB, mail, in-memory)
    - `feature/`: Feature modules (FIDO, mail parser)
    - `graphql/`: GraphQL schema and generated code

- `frontend/`: Kotlin/JS and Android applications
    - `app/`: Platform-specific entry points (JS + Android)
    - `common/base/`: Core utilities
    - `common/ui/`: Shared UI components
    - `common/viewmodel/`: ViewModels
    - `common/usecase/`: Business logic layer
    - `common/navigation/`: Navigation logic
    - `common/graphql/`: Apollo client and schema
    - `common/di/`: Frontend DI (Koin)
    - `common/feature/`: Feature modules (webauth, localstore)

- `shared/`: Code shared between backend and frontend
- `build-logic/`: Convention plugins and version catalog

# コーディングガイドライン

@docs/coding_style.md

# 実装時の注意

- メール、通知のパーサのテストは書かない。パーサで例をコメントする時はマスキングやダミーデータを使用し、ユーザーから依頼された文章を含むことを禁止する。
- すべての応答、説明、およびコミットメッセージは日本語で行う。
- アイコンリソース
  - `material-icons-extended`は使用禁止
  - svg(xml)のみ使用する
  - `frontend.common.ui`モジュールに配置する
- ツールを使用する場合はプロジェクト内では絶対pathの使用は禁止。相対pathを使う

# フロントエンドの具体的な実装の指示

- MVVMを使用する
- Composable内で早期returnを使用しないif-else,whenで分岐させる

## ViewModel - UiState

- UiStateがUIとの接点、ViewModelの関数を呼ばない。
- ViewModelStateに全ての情報を詰め込む。ViewModelStateをデータソースとしてUiStateを作る。
- combineを使用しているのは間違ったコードの兆候。大体ViewModelStateに入れるべき。

## UiState - UI

- UIからのイベント取得はUiState内のイベントハンドラーを使用する。Lambdaを使用しない。`@Immutable interface`を使用する

## UI - ViewModel - UiState
- 表示に必要ない値はUiStateに入れない
- UIに紐づく処理に必要な値はViewModelで持ち、EventのInterfaceの実装で実態を持ち、処理する。
  - `class EventImpl(val data: Int): Event { fun onClick() { println(data) } }` 

## Paging
ApolloのPagingは`updateOperation()`を使用し、最初のOperationに連結し、`watch()`しているだけでデータが流れてくるようにする。

# ビルドとテスト

- 実装が完了したらフォーマットの後にビルドをする。
- Gradleのビルドコマンドを複数同時に並列で実行しない。Gradleのロックが競合する。

## ビルド

- コードを編集した後は必ず以下を実行してエラーを確認する。
- モジュール個別のビルドは禁止

```sh
./gradlew :backend:assemble :frontend:app:jsBrowserDevelopmentWebpack :frontend:app:assembleDebug --quiet
```

ビルドができたらFormatする

```sh
./gradlew ktlintFormat
```

## テスト

- Android依存なし: Kotest (JUnit 5)
- Android依存やRobolectricあり: JUnit 4

```sh
./gradlew allTests --quiet
```

## 個別ビルドコマンド

```shell
// Backend
./gradlew :backend:build

// Frontend - Android App
./gradlew :frontend:app:assembleDebug

// Frontend - JS
./gradlew :frontend:app:jsBrowserDevelopmentWebpack

// Production JS
./gradlew :frontend:app:jsBrowserProductionWebpack
```

# コード生成

Graphqlスキーマ生成
skill: @.claude/skills/download-graphql-schema

`GraphQL (Front)`の再生成

```shell
./gradlew generateApolloSources
```
