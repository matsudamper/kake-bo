package net.matsudamper.money.backend.lib

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
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
            .decodeToString()

        return Base64.getEncoder()
            .encodeToString("${randomText}.$hashedRandomText".encodeToByteArray())
    }

    /**
     * @return isSuccess
     */
    fun validateChallenge(challenge: String): Boolean {
        val decoded = Base64.getDecoder().decode(challenge).decodeToString()

        val randomText: String
        val hashedRandomText: String
        decoded.split(".").also {
            if (it.size != 2) {
                throw IllegalArgumentException("Invalid challenge: [$decoded]")
            }
            randomText = it[0]
            hashedRandomText = it[1]
        }

        val hashedRandomText2 = mac.doFinal(randomText.encodeToByteArray())
            .decodeToString()
        return hashedRandomText == hashedRandomText2
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
