package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.backend.app.interfaces.element.ImapConfig
import net.matsudamper.money.element.UserId

interface UserConfigRepository {

    fun getImapConfig(userId: UserId): ImapConfig?
    fun updateImapConfig(userId: UserId, host: String?, port: Int?, password: String?, userName: String?): Boolean

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
