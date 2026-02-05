# プロジェクトについて

メールをパースして登録する家計簿アプリ。決済メールを中心とした家計簿ソフト。

- メールサーバーからメールを取得し、パースして決済情報を登録する
- 資産管理機能は無い（使用用途の管理だけを行い、財産の管理は行わない）

## Tech Stack

**Backend**:
- Ktor server framework (Netty)
- GraphQL (graphql-java, GraphQL Java Kickstart)
- MariaDB for persistence
- Redis (optional) for sessions/challenges
- FIDO for authentication

**Frontend**:
- Kotlin/JS for web
- Jetpack Compose Multiplatform (Web + Android)
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

### メールのパース
@.claude/skills/add-email-parser

### GraphQL スキーマ

- Backend: `backend/graphql/src/commonMain/resources/graphql/`
  - `schema.graphqls` - メインスキーマ
  - `user_query.graphqls`, `user_mutation.graphqls` - ユーザー操作
  - `money_usage.graphqls`, `money_usage_analytics.graphqls` - 家計簿機能
  - `fido_info.graphqls` - FIDO認証
  - `imported_mail.graphqls` - メールインポート

### FIDO認証

`backend/feature/fido/`モジュールでWebAuthn4Jを使用。
- `FidoAuthenticator.kt` - データモデル
- `AuthenticatorConverter.kt` - 変換ロジック
- `Auth4JModel.kt` - WebAuthn4J統合

# コーディングガイドライン

@docs/coding_style.md

# Claude Code Web
Claude Code WebではGradleの依存がダウンロードでエラーになります。
https://github.com/anthropics/claude-code/issues/13372

# ビルドとテスト

実装が完了したらフォーマットの後にビルドをして。

## ビルド

コードを編集した後は必ず以下を実行してエラーを確認してください。

```sh
./gradlew assemble assembleDebug -x jsBrowserProductionWebpack
```

## フォーマット

ビルドができたらFormatしてください。

```sh
./gradlew ktlintFormat
```

## テスト

テストフレームワーク: Kotest (JUnit 5), mockk

```sh
./gradlew allTests
```

## CI/CD

GitHub Actionsで自動ビルド・テスト（`.github/workflows/`）:
- **build.yml**: push/PR時に`ktlintCheck`, ビルド, `allTests`を実行
- **release-docker.yml**: タグpush時にDockerイメージをghcr.ioへプッシュ

## 個別ビルド

### Backend

```shell
./gradlew :backend:build
```

### Frontend - Android App

```shell
./gradlew :frontend:app:assembleDebug
```

### Frontend - JS

```shell
./gradlew :frontend:app:jsBrowserDevelopmentWebpack
```

### Production JS

```shell
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

サーバー側の環境変数は`IS_DEBUG=true`は必須。

```shell
./gradlew :frontend:common:graphql:schema:downloadSchema
```
