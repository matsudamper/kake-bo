package net.matsudamper.money.backend.logic

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import net.matsudamper.money.backend.base.ServerEnv

class PasswordManager(
    private val algorithmName: String = "PBKDF2WithHmacSHA512",
    private val saltByte: Int = 32,
    private val keyByte: Int = 512,
    private val iterationCount: Int = 100000,
    private val pepper: String = ServerEnv.userPasswordPepper,
) {
    private val secretKeyFactory = SecretKeyFactory.getInstance(algorithmName)
    private val bases64Encoder = Base64.getEncoder()

    fun create(password: String): CreateResult {
        val salt = createSalt()
        val spec = createKeySpec(
            password = password,
            salt = salt,
        )
        val hashedPassword = bases64Encoder.encodeToString(secretKeyFactory.generateSecret(spec).encoded)

        return CreateResult(
            salt = createSalt(),
            hashedPassword = hashedPassword,
            algorithm = algorithmName,
            iterationCount = iterationCount,
            keyLength = keyByte,
        )
    }

    private fun createKeySpec(
        password: String,
        salt: ByteArray,
    ): PBEKeySpec {
        return PBEKeySpec(
            password.plus(pepper).toCharArray(),
            salt,
            iterationCount,
            keyByte,
        )
    }

    private fun createSalt(): ByteArray {
        return ByteArray(saltByte).also { byteArray ->
            SecureRandom().nextBytes(byteArray)
        }
    }

    class CreateResult(
        val salt: ByteArray,
        val algorithm: String,
        val iterationCount: Int,
        val keyLength: Int,
        val hashedPassword: String,
    )
}
