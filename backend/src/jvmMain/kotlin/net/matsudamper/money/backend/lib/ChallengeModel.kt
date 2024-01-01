package net.matsudamper.money.backend.lib

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64
import net.matsudamper.money.backend.base.ServerEnv

/**
 * https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#alternative-using-a-double-submit-cookie-pattern
 */
class ChallengeModel(
    private val challengeSecretByteArray: ByteArray = ServerEnv.challengeSecret.toByteArray(),
) {
    private val mac = Mac.getInstance(ALGORITHM).also { mac ->
        mac.init(
            SecretKeySpec(
                challengeSecretByteArray,
                ALGORITHM,
            ),
        )
    }

    fun generateChallenge(): String {
        val randomText = (0 until 30)
            .map { challengeChars.random() }
            .joinToString("")

        val hashedRandomText = mac.doFinal(randomText.encodeToByteArray())

        return "${randomText.encodeBase64()}.${hashedRandomText.encodeBase64()}"
    }

    /**
     * @return isSuccess
     */
    fun validateChallenge(challenge: String): Boolean {
        val randomText: String
        val hashedRandomText: ByteArray
        challenge.split(".").also {
            if (it.size != 2) {
                throw IllegalArgumentException("Invalid challenge: [$challenge]")
            }
            randomText = it[0].decodeBase64String()
            hashedRandomText = it[1].decodeBase64Bytes()
        }

        val hashedRandomText2 = mac.doFinal(randomText.encodeToByteArray())
        return hashedRandomText.contentEquals(hashedRandomText2)
    }

    companion object {
        private const val ALGORITHM = "HmacSHA256"
        private val challengeChars = buildList {
            addAll('a'..'z')
            addAll('A'..'Z')
            addAll('0'..'9')
        }
    }
}
