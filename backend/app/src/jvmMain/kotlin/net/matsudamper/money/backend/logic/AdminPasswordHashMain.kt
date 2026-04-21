package net.matsudamper.money.backend.logic

import java.util.Base64

/**
 * 管理者パスワードをhash化してコンソールに出力するローカル実行用ツール。
 *
 * DBの `admin_passwords` テーブルに登録するhash化パスワードとsaltを生成するために使用する。
 * 出力されたDB INSERT文を手動でDBに実行することで管理者パスワードを設定できる。
 *
 * 実行方法:
 * - 引数にパスワードを渡す場合: `./gradlew :backend:app:run --args='<password>'`
 * - 引数なしの場合: 対話的に入力を求める
 *
 * 環境変数 `USER_PASSWORD_PEPPER` が必要。
 */
fun main(args: Array<String>) {
    val password = if (args.isNotEmpty()) {
        args[0]
    } else {
        print("パスワードを入力してください: ")
        readlnOrNull() ?: run {
            println("パスワードが入力されませんでした")
            return
        }
    }

    val passwordManager = PasswordManager()
    val result = passwordManager.create(
        password = password,
        keyByteLength = 512,
        iterationCount = 100000,
        saltByteLength = 32,
        algorithm = IPasswordManager.Algorithm.PBKDF2WithHmacSHA512,
    )

    val base64Encoder = Base64.getEncoder()
    println("=== 管理者パスワードhash化結果 ===")
    println("password_hash: ${result.hashedPassword}")
    println("salt (base64): ${base64Encoder.encodeToString(result.salt)}")
    println("salt (hex): ${result.salt.joinToString("") { "%02x".format(it) }}")
    println("algorithm: ${result.algorithm}")
    println("iteration_count: ${result.iterationCount}")
    println("key_length: ${result.keyLength}")
    println()
    println("=== DB INSERT文 ===")
    println(
        "INSERT INTO admin_passwords (password_hash, salt, algorithm, iteration_count, key_length) " +
            "VALUES ('${result.hashedPassword}', " +
            "X'${result.salt.joinToString("") { "%02x".format(it) }}', " +
            "'${result.algorithm}', " +
            "${result.iterationCount}, " +
            "${result.keyLength});",
    )
}
