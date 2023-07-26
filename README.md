# 家計簿
決済メールを中心とした家計簿ソフト。購入履歴を取得するためにパスワードを外部サービスに登録したくない為に作成された。

# 使用技術
- フロント
  - Jetbrains Compose Multiplatform Web
  - Jetbrains Compose HTML
- バックエンド
  - Ktor
- インターフェース
  - GraphQL
 
# 準備するもの
- メールサーバー
  - ここに購入メールを転送する
- MariaDB
  - [テーブルを作成する](https://github.com/matsudamper/kake-bo/tree/70e838f4d1c31460ccb110290cb0a3343b124858/backend/db/src/jvmMain/resources/sql)

# 動作手順
まずはフロントをビルドする。その後にバックエンドを起動する。
```shell
 ./gradlew :frontend:jsApp:jsBrowserDevelopmentWebpack
```
必要な環境変数は[ServerEnv.kt](https://github.com/matsudamper/kake-bo/blob/563272f802d15d6620432a53ada88fbdd5cf9561/backend/base/src/jvmMain/java/net/matsudamper/money/backend/base/ServerEnv.kt)を参照

# Update Code
DB
```shell
./gradlew generateDbCode
```

GraphQl(Front)
```shell
./gradlew generateApolloSources
```
