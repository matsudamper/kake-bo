# 家計簿

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
