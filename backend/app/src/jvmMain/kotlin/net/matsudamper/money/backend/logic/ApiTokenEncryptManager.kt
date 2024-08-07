package net.matsudamper.money.backend.logic

import java.security.SecureRandom
import java.util.Base64

class ApiTokenEncryptManager {
    private val v1EncryptInfo = EncryptInfo(
        keyByteLength = 512,
        iterationCount = 100000,
        algorithmName = "PBKDF2WithHmacSHA512",
        salt = ByteArray(1),
    )

    fun getEncryptInfo(apiToken: String): EncryptInfo? {
        return if (apiToken.startsWith(V1_APIKEY_PREFIX)) {
            v1EncryptInfo
        } else {
            null
        }
    }

    fun createApiToken(): CreateTokenResult {
        val token = buildList {
            add(V1_APIKEY_PREFIX)
            add(
                Base64.getEncoder().encodeToString(
                    ByteArray(256).also { SecureRandom().nextBytes(it) },
                ),
            )
        }.joinToString("")

        return CreateTokenResult(
            token = token,
            encryptInfo = v1EncryptInfo,
        )
    }

    data class CreateTokenResult(
        val token: String,
        val encryptInfo: EncryptInfo,
    )

    data class EncryptInfo(
        val keyByteLength: Int,
        val iterationCount: Int,
        val algorithmName: String,
        val salt: ByteArray,
    )

    companion object {
        private const val V1_APIKEY_PREFIX = "apikey_"
    }
}
