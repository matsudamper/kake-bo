package net.matsudamper.money.backend.app.interfaces

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

    /**
     * ユーザーに設定されたタイムゾーンオフセットを分単位で返す。
     *
     * 設定が未作成の場合は `0` を返す。
     */
    fun getTimezoneOffset(userId: UserId): Int

    /**
     * ユーザーのタイムゾーンオフセットを更新する。
     *
     * `offsetMinutes` は [TIMEZONE_OFFSET_RANGE] の範囲で指定する。
     */
    fun updateTimezoneOffset(userId: UserId, offsetMinutes: Int): Boolean

    companion object {
        val TIMEZONE_OFFSET_RANGE: IntRange = -720..840
    }

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
