package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.UserId

interface ApiTokenRepository {
    fun registerToken(id: UserId, keyLength: Int, iterationCount: Int, hashedToken: String, salt: ByteArray)
    fun verifyToken(id: UserId, hashedToken: String): VerifyTokenResult

    data class VerifyTokenResult(
        val userId: UserId,
        val permissions: String,
    )
}
