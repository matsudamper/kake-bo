package net.matsudamper.money.backend.app.interfaces

interface AdminLoginRepository {
    fun getLoginEncryptInfo(): LoginEncryptInfo?

    fun verifyPassword(hashedPassword: String): Boolean

    class LoginEncryptInfo(
        val salt: ByteArray,
        val algorithm: String,
        val iterationCount: Int,
        val keyLength: Int,
    )
}
