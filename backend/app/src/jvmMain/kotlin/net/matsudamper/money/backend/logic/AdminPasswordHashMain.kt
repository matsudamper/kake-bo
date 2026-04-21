package net.matsudamper.money.backend.logic

/**
 * 管理者パスワードをhash化して環境変数の設定値をコンソールに出力するローカル実行用ツール。
 *
 * 出力された値をサーバーの環境変数に設定することで管理者パスワードを設定できる。
 *
 * 設定が必要な環境変数:
 * - `ADMIN_PASSWORD_HASH`: hash化されたパスワード（Base64）
 * - `ADMIN_PASSWORD_SALT`: ソルト（hex文字列）
 * - `ADMIN_PASSWORD_ALGORITHM`: アルゴリズム名（例: PBKDF2WithHmacSHA512）
 * - `ADMIN_PASSWORD_ITERATION_COUNT`: イテレーション回数
 * - `ADMIN_PASSWORD_KEY_LENGTH`: キー長さ（ビット数）
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

    println("=== 管理者パスワードhash化結果 ===")
    println("ADMIN_PASSWORD_HASH=${result.hashedPassword}")
    println("ADMIN_PASSWORD_SALT=${result.salt.joinToString("") { "%02x".format(it) }}")
    println("ADMIN_PASSWORD_ALGORITHM=${result.algorithm}")
    println("ADMIN_PASSWORD_ITERATION_COUNT=${result.iterationCount}")
    println("ADMIN_PASSWORD_KEY_LENGTH=${result.keyLength}")
}
