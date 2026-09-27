package net.matsudamper.money.backend.base

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * DBに保存する秘密情報をAES-256-GCMで暗号化する。
 * 暗号文は `v1:` に続けて nonce と暗号文(認証タグ込み)を連結したものをBase64で表す。
 */
public class DbSecretCipher private constructor(
    private val key: SecretKey,
) {
    private val secureRandom = SecureRandom()

    /**
     * @param associatedData 暗号文を別の行や列へコピーしても復号できないよう、保存先を表す値を渡す
     */
    public fun encrypt(
        plainText: String,
        associatedData: String,
    ): String {
        val nonce = ByteArray(NONCE_BYTE_LENGTH).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BIT_LENGTH, nonce))
        cipher.updateAAD(associatedData.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return VERSION_PREFIX + Base64.getEncoder().encodeToString(nonce + encrypted)
    }

    public fun decrypt(
        encryptedText: String,
        associatedData: String,
    ): String {
        require(encryptedText.startsWith(VERSION_PREFIX)) { "未対応の暗号文形式です" }
        val bytes = Base64.getDecoder().decode(encryptedText.removePrefix(VERSION_PREFIX))
        require(bytes.size > NONCE_BYTE_LENGTH) { "暗号文が短すぎます" }
        val nonce = bytes.copyOfRange(0, NONCE_BYTE_LENGTH)
        val encrypted = bytes.copyOfRange(NONCE_BYTE_LENGTH, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BIT_LENGTH, nonce))
        cipher.updateAAD(associatedData.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    public companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BYTE_LENGTH = 32
        private const val NONCE_BYTE_LENGTH = 12
        private const val TAG_BIT_LENGTH = 128
        private const val VERSION_PREFIX = "v1:"

        /**
         * @param base64Key 32バイトの乱数をBase64にした値
         */
        public fun fromBase64Key(base64Key: String): DbSecretCipher {
            val keyBytes = Base64.getDecoder().decode(base64Key.trim())
            require(keyBytes.size == KEY_BYTE_LENGTH) { "暗号鍵は${KEY_BYTE_LENGTH}バイトをBase64にした値を指定してください" }
            return DbSecretCipher(SecretKeySpec(keyBytes, "AES"))
        }
    }
}
