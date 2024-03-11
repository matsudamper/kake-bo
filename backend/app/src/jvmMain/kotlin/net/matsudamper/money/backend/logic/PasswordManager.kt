package net.matsudamper.money.backend.logic

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import net.matsudamper.money.backend.base.ServerEnv

interface IPasswordManager {
    fun create(password: String): CreateResult

    fun getHashedPassword(
        password: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
    ): ByteArray

    class CreateResult(
        val salt: ByteArray,
        val algorithm: String,
        val iterationCount: Int,
        val keyLength: Int,
        val hashedPassword: String,
    )
}

class PasswordManager(
    private val algorithmName: String = "PBKDF2WithHmacSHA512",
    private val saltByteLength: Int = 32,
    private val keyByteLength: Int = 512,
    private val iterationCount: Int = 100000,
    private val pepper: String = ServerEnv.userPasswordPepper,
) : IPasswordManager {
    private val secretKeyFactory = SecretKeyFactory.getInstance(algorithmName)
    private val bases64Encoder = Base64.getEncoder()

    override fun create(password: String): IPasswordManager.CreateResult {
        val salt = createSalt()
        val spec = createKeySpec(
            password = password,
            salt = salt,
            iterationCount = iterationCount,
            keyByteLength = keyByteLength,
        )
        val hashedPassword = bases64Encoder.encodeToString(secretKeyFactory.generateSecret(spec).encoded)

        return IPasswordManager.CreateResult(
            salt = createSalt(),
            hashedPassword = hashedPassword,
            algorithm = algorithmName,
            iterationCount = iterationCount,
            keyLength = keyByteLength,
        )
    }

    override fun getHashedPassword(password: String, salt: ByteArray, iterationCount: Int, keyLength: Int): ByteArray {
        val keySpec = createKeySpec(
            password = password,
            salt = salt,
            iterationCount = iterationCount,
            keyByteLength = keyLength,
        )
        return secretKeyFactory.generateSecret(keySpec).encoded
    }

    private fun createKeySpec(
        password: String,
        salt: ByteArray,
        iterationCount: Int,
        keyByteLength: Int,
    ): PBEKeySpec {
        return PBEKeySpec(
            password.plus(pepper).toCharArray(),
            salt,
            iterationCount,
            keyByteLength,
        )
    }

    private fun createSalt(): ByteArray {
        return ByteArray(saltByteLength).also { byteArray ->
            SecureRandom().nextBytes(byteArray)
        }
    }
}
