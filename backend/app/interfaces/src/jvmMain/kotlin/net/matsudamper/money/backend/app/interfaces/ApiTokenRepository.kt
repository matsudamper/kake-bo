package net.matsudamper.money.backend.app.interfaces

import java.time.Instant
import net.matsudamper.money.element.UserId

interface ApiTokenRepository {
    fun registerToken(id: UserId, keyLength: Int, iterationCount: Int, hashedToken: String, salt: ByteArray)
    fun verifyToken(id: UserId, hashedToken: String): VerifyTokenResult
    fun getApiTokens(id: UserId): List<ApiToken>

    data class ApiToken(
        val name: String,
        val expiredAt: Instant,
    )

    data class VerifyTokenResult(
        val userId: UserId,
        val permissions: String,
    )
}
