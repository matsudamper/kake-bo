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

`backend/feature/service_mail_parser`モジュールの`net/matsudamper/money/backend/mail/parser/services`にメールをパースするコードがあります。
`net/matsudamper/money/backend/mail/parser/MailParser`に登録して使います。パースを追加する場合は全てのパース方法を確認してから実装してください。

# コーディングガイドライン

以下に従ってください
https://kotlinlang.org/docs/coding-conventions.html

Composeに関しては以下を参照してください
https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md

## コードスタイル

基本は`.editorconfig`を参照してください。

## コメント

コメントはなるべく使わず、関数名や変数名で説明できないか検討してください。コメントはコードの説明であってはなりません。コメントが必要な時はWhyを書く時です。
コメントは日本語で書いてください。日本語で書くのはコメントだけです。

## スコープ関数

戻り値を使用しない場合に戻り値ありのスコープ関数を使用しないでください。

NG

```kotlin
run {
    // 処理
}
```

OK

```kotlin
apply {
    // 処理
}
```

## 処理が長い場合のスコープ関数とnull合体演算子

NG

```kotlin
val value = hoge?.let {
    val one = fuga(it)
    /* ~2行以上の処理~ */
} ?: piyo
```

OK

```kotlin
val value = if (hoge != null) {
    fuga(hoge)
} else {
    null
} ?: piyo
```

## nullableとスコープ関数

戻り値を使用せず、関数を実行するだけの場合においてはifを優先して使用してください。

NG

```kotlin
hoge?.also { fuga(it) }
```

OK

```kotlin
if (hoge != null) {
    fuga(hoge)
}
```

## null合体演算子

null合体演算子使用しなくて良いものは使用しないでください。Listでも同様に`orEmpty()`を使用してください。

NG

```kotlin
str ?: ""
```

OK

```kotlin
str.orEmpty()
```

### emptyList

SetやMapも同様。

NG

```kotlin
emptyList()
```

OK

```kotlin
listOf()
```

## indexOf

`indexOf`を使用する時は`takeIf { it >= 0 }`を使用し、-1を意識しないで良いようにしてください。

## デフォルト引数

デフォルト引数はなるべく使用しないでください。既にデフォルト引数が使われている場合は、デフォルト引数を使用しても構いません。
ComposeではModifierだけがデフォルト引数が使用されていますが、これは特別なので倣わないでください。

## Mutable

`var`はなるべく使わず、`val`を使用してください。どうしてもvarを使う必要がある、varを使わないと可読性が落ちる場合は関数に切り出せないか検討してください。
Composeの`mutableStateOf`は例外で、`var`を使用しても構いませんが、使わなくても良いかは検討してください。
MutableListやMutableMapはなるべく使わず、buildList等を使うことも検討してください

## Force unwrap

`!!`を使用しないでください。nullチェックを行ってください。
モジュールが違うなどでnullチェックをしてもnullになる場合はvalに代入し直して直してください。

## 定義の順番

変数/関数の順番に並べてください。
public/internal/privateの順番に並べてください。

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
