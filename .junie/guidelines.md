# コーディングガイドライン
以下に従ってください
https://kotlinlang.org/docs/coding-conventions.html

Composeに関しては以下を参照してください
https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md

## コードスタイル

基本は`.editorconfig`を参照してください。

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

## 閭里が長い場合のスコープ関数とnull合体演算子

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

## コメント

コメントはなるべく使わず、関数名や変数名で説明できないか検討してください。コメントはコードの説明であってはなりません。コメントが必要な時はWhyを書く時です。
コメントは日本語で書いてください。日本語で書くのはコメントだけです。

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

# 確認

コードを編集した後は必ず以下を実行してエラーを確認してください

```sh
./gradlew assemble assembleDebug -x jsBrowserProductionWebpack
```

ビルドができたらFormatしてください

```sh
./gradlew ktlintFormat
```

