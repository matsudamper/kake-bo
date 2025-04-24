package net.matsudamper.money.backend.app.interfaces

import java.time.Instant
import net.matsudamper.money.element.UserId

interface ApiTokenRepository {
    fun registerToken(
        id: UserId,
        name: String,
        hashedToken: String,
    )

    fun verifyToken(hashedToken: ByteArray): VerifyTokenResult?
    fun getApiTokens(id: UserId): List<ApiToken>

    data class ApiToken(
        val id: ApiTokenId,
        val name: String,
        val expiredAt: Instant?,
    )

    data class VerifyTokenResult(
        val userId: UserId,
        val permissions: String,
    )
}
