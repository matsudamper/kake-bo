# プロジェクトについて

メールをパースして登録する家計簿アプリ

# ファイルの場所

- `backend/`: Ktor server application
    - `app/`: Main application and GraphQL data loaders

- `frontend/`: Kotlin/JS and Android applications
    - `app/`: Platform-specific entry points (JS + Android)
- `shared/`: Code shared between backend and frontend
- `build-logic/`: Convention plugins and version catalog

## メールのパース

`backend/feature/service_mail_parser`モジュールの`net/matsudamper/money/backend/mail/parser/services`にメールをパースするコードがあります。
`net/matsudamper/money/backend/mail/parser/MailParser`に登録して使います。パースを追加する場合は全てのパース方法を確認してから実装してください。

# コーディングルール

読んで周りに合わせて。コードを読んでわかるコメントは禁止(基本禁止)。

# ビルドとテスト

実装が完了したらフォーマットの後にビルドをして。

### format

```shell
./gradlew ktlintFormat
```

### backend

```shell
./gradlew :backend:build
```

## frontend

アプリ

```shell
./gradlew :frontend:app:assembleDebug
```

JS

```shell
./gradlew :frontend:app:jsBrowserDevelopmentWebpack
```
