package net.matsudamper.money.backend.app.interfaces

import java.time.Instant
import net.matsudamper.money.element.ApiTokenId
import net.matsudamper.money.element.UserId

interface ApiTokenRepository {
    fun registerToken(
        id: UserId,
        name: String,
        hashedToken: String,
    )

    fun verifyToken(hashedToken: ByteArray): VerifyTokenResult?
    fun getApiTokens(id: UserId): List<ApiToken>

    /**
     * @return 削除が成功したか
     */
    fun deleteToken(userId: UserId, apiTokenId: ApiTokenId): Boolean

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
