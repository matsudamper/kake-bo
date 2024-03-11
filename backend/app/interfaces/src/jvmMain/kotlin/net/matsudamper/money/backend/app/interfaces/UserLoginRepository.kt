package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.UserId

interface UserLoginRepository {
    sealed interface Result {
        data class Success(val uerId: UserId) : Result

        data object Failure : Result
    }

    class LoginEncryptInfo(
        val salt: ByteArray,
        val algorithm: String,
        val iterationCount: Int,
        val keyLength: Int,
    )

    fun login(
        userName: String,
        hashedPassword: ByteArray,
    ): Result

    fun getLoginEncryptInfo(userName: String): LoginEncryptInfo?
}
