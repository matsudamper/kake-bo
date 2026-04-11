package net.matsudamper.money.frontend.android.feature.notificationusage

import java.security.MessageDigest

internal object NotificationUsageKeyBuilder {
    private const val HEX_CHARS: String = "0123456789abcdef"

    fun build(
        notificationKey: String,
        packageName: String,
        text: String,
        postedAtEpochMillis: Long,
    ): String {
        val source = buildString {
            appendValue(notificationKey)
            appendValue(packageName)
            appendValue(text)
            appendValue(postedAtEpochMillis.toString())
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .toHexString()
        return "$notificationKey#$digest"
    }

    private fun StringBuilder.appendValue(value: String) {
        append(value.length)
        append(':')
        append(value)
        append('\n')
    }

    private fun ByteArray.toHexString(): String {
        return buildString(size * 2) {
            this@toHexString.forEach { byte ->
                val value = byte.toInt() and 0xff
                append(HEX_CHARS[value shr 4])
                append(HEX_CHARS[value and 0x0f])
            }
        }
    }
}
