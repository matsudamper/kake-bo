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


## null合体演算子

StringやListのorEmpty等でnull合体演算子使用しなくて良いものは使用しないでください。

### emptyList
Kotlinのemptyは使用しない。listOf()やmapOf()を使用する

## indexOf

`indexOf`を使用する時は`takeIf { it >= 0 }`を必ず使用する

## デフォルト引数

デフォルト引数はなるべく使用しない。既にデフォルト引数が使われている場合は、デフォルト引数を使用しても良い。
ComposeではModifierだけがデフォルト引数が使用されていますが、これは特別なので倣わないでください。

## Mutable

`var`はなるべく使わず、`val`を使用してください。どうしてもvarを使う必要がある、varを使わないと可読性が落ちる場合は関数に切り出せないか検討してください。
Composeの`mutableStateOf`は例外で、`var`を使用しても構いませんが、使わなくても良いかは検討してください。
MutableListやMutableMapはなるべく使わず、buildList等を使うことも検討してください

## Suppress
Suppressの変わりにOptInを使用する

## Force unwrap

`!!`を使用しないでください。nullチェックを行ってください。
モジュールが違うなどでnullチェックをしてもnullになる場合はvalに代入し直して直してください。

## 定義の順番

変数/関数の順番に並べてください。
public/internal/privateの順番に並べてください。

## その他

- 拡張関数を使用しない