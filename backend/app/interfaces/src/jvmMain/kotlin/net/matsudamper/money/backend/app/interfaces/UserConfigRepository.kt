package net.matsudamper.money.backend.app.interfaces

import java.time.ZoneOffset
import net.matsudamper.money.backend.app.interfaces.element.ImapConfig
import net.matsudamper.money.element.UserId

interface UserConfigRepository {
    fun getImapConfig(userId: UserId): ImapConfig?

    fun updateImapConfig(
        userId: UserId,
        host: String?,
        port: Int?,
        password: String?,
        userName: String?,
    ): Boolean

    fun getTimezoneOffset(userId: UserId): ZoneOffset?

    fun updateTimezoneOffset(userId: UserId, offset: ZoneOffset): Boolean

    fun getGeminiApiKey(userId: UserId): String?

    /**
     * @param apiKey nullの場合は削除する
     */
    fun updateGeminiApiKey(userId: UserId, apiKey: String?): Boolean

    sealed interface Optional<T> {
        class None<T> : Optional<T>

        class HasValue<T>(val value: T) : Optional<T>

        fun hasValue(block: (T) -> Unit) {
            when (this) {
                is HasValue -> block(value)
                is None -> Unit
            }
        }

        companion object {
            fun <T> none() = None<T>()
        }
    }
}
