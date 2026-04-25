# プロジェクトについて

メールをパースして登録する家計簿アプリ。決済メールを中心とした家計簿ソフト。

- メールサーバーからメールを取得し、パースして決済情報を登録する
- 資産管理機能は無い（使用用途の管理だけを行い、財産の管理は行わない）

## Tech Stack

**Backend**:
- Ktor server framework
- GraphQL (graphql-java, GraphQL Java Kickstart)
- MariaDB for persistence
- Lettuce for sessions/challenges
- FIDO for authentication(WebAuthn4J)

**Frontend**:
- Kotlin/JS for web
- Jetpack Compose Multiplatform (WASM + Android)
- Apollo Client for GraphQL
- Compose HTML

## Architecture

### Module Structure

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

### GraphQL スキーマ

- Backend: `backend/graphql/src/commonMain/resources/graphql/`

# コーディングガイドライン

@docs/coding_style.md

パーサのテストは書かない。パーサで例をコメントする時はマスキングやダミーデータを使用し、ユーザーから依頼された文章を含むことを禁止する。

## ViewModel
ViewModelでUiStateをcombineで使用しているのは間違ったコードの兆候。大体ViewModelStateに入れれば解決する。

## フロントエンドのアーキテクチャ
- MVVMを使用する
- UIの更新はUiState必ず経由する
- UIからのイベント取得はUiState内のイベントハンドラーを使用する

# Coding Agent
すべての応答、説明、およびコミットメッセージは日本語で行ってください。

### スクリーンショットに関するルール
スクリーンショットは撮れないため、スクリーンショットの試行をしないでください。

# ビルドとテスト

実装が完了したらフォーマットの後にビルドをして。

## 注意事項

Gradleのビルドコマンドを複数同時に並列で実行しないでください。Gradleはロックファイルを使用するため、並列実行するとビルドが失敗します。ビルド・フォーマット・テストは必ず順番に1つずつ実行してください。

## ビルド

コードを編集した後は必ず以下を実行してエラーを確認してください。

```sh
./gradlew :backend:assemble :frontend:app:jsBrowserDevelopmentWebpack :frontend:app:assembleDebug --quiet
```

## フォーマット

ビルドができたらFormatしてください。

```sh
./gradlew ktlintFormat
```

## テスト

### テストフレームワーク

- Android依存なし: Kotest (JUnit 5)
- Android依存あり: JUnit 4

### コマンド
```sh
./gradlew allTests --quiet
```

## CI/CD

GitHub Actionsで自動ビルド・テスト（`.github/workflows/`）:
- **build.yml**: push/PR時に`ktlintCheck`, ビルド, `allTests`を実行
- **release-docker.yml**: タグpush時にDockerイメージをghcr.ioへプッシュ

## ビルドコマンド

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

## コード生成

### DB

```shell
./gradlew generateDbCode
```

### GraphQL (Front)

```shell
./gradlew generateApolloSources
```

### Download Schema

@.claude/skills/download-graphql-schema
