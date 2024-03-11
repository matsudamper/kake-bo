package net.matsudamper.money.backend.logic

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import net.matsudamper.money.backend.base.ServerEnv

interface IPasswordManager {
    fun create(
        password: String,
        keyByteLength: Int,
        iterationCount: Int,
        saltByteLength: Int,
        algorithm: Algorithm,
    ): CreateResult

    fun getHashedPassword(
        password: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
        algorithm: Algorithm,
    ): ByteArray

    class CreateResult(
        val salt: ByteArray,
        val algorithm: String,
        val iterationCount: Int,
        val keyLength: Int,
        val hashedPassword: String,
    )

    enum class Algorithm(val algorithmName: String) {
        PBKDF2WithHmacSHA512("PBKDF2WithHmacSHA512"),
    }
}

class PasswordManager(
    private val pepper: String = ServerEnv.userPasswordPepper,
) : IPasswordManager {
    private val bases64Encoder = Base64.getEncoder()

    override fun create(
        password: String,
        keyByteLength: Int,
        iterationCount: Int,
        saltByteLength: Int,
        algorithm: IPasswordManager.Algorithm,
    ): IPasswordManager.CreateResult {
        val salt = createSalt(saltByteLength)
        val spec = createKeySpec(
            password = password,
            salt = salt,
            iterationCount = iterationCount,
            keyByteLength = keyByteLength,
        )
        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm.algorithmName)
        val hashedPassword = bases64Encoder.encodeToString(secretKeyFactory.generateSecret(spec).encoded)

        return IPasswordManager.CreateResult(
            salt = salt,
            hashedPassword = hashedPassword,
            algorithm = algorithm.algorithmName,
            iterationCount = iterationCount,
            keyLength = keyByteLength,
        )
    }

    override fun getHashedPassword(
        password: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
        algorithm: IPasswordManager.Algorithm,
    ): ByteArray {
        val keySpec = createKeySpec(
            password = password,
            salt = salt,
            iterationCount = iterationCount,
            keyByteLength = keyLength,
        )
        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm.algorithmName)
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

    private fun createSalt(saltByteLength: Int): ByteArray {
        return ByteArray(saltByteLength).also { byteArray ->
            SecureRandom().nextBytes(byteArray)
        }
    }
}
