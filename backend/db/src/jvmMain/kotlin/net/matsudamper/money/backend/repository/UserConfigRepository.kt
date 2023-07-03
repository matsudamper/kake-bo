package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.ImapConfig
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JUserImapSettings
import org.jooq.impl.DSL

class UserConfigRepository {
    /**
     * @return isSuccess
     */
    fun updateImapConfig(
        userId: UserId,
        host: String?,
        port: Int?,
        password: String?,
        userName: String?,
    ): Boolean {
        return runCatching {
            val userImap = JUserImapSettings.USER_IMAP_SETTINGS
            DbConnection.use {
                host?.also { host ->
                    DSL.using(it)
                        .insertInto(userImap)
                        .set(userImap.USER_ID, userId.id)
                        .onDuplicateKeyUpdate()
                        .set(userImap.HOST, host)
                        .execute()
                }
                port?.also { port ->
                    DSL.using(it)
                        .insertInto(userImap)
                        .set(userImap.USER_ID, userId.id)
                        .onDuplicateKeyUpdate()
                        .set(userImap.PORT, port)
                        .execute()
                }
                password?.also { password ->
                    DSL.using(it)
                        .insertInto(userImap)
                        .set(userImap.USER_ID, userId.id)
                        .onDuplicateKeyUpdate()
                        .set(userImap.PASSWORD, password)
                        .execute()
                }
                userName?.also { userName ->
                    DSL.using(it)
                        .insertInto(userImap)
                        .set(userImap.USER_ID, userId.id)
                        .onDuplicateKeyUpdate()
                        .set(userImap.USE_NAME, userName)
                        .execute()
                }
            }
        }.fold(
            onSuccess = { true },
            onFailure = { false },
        )
    }

    fun getImapConfig(userId: UserId): ImapConfig? {
        return runCatching {
            val userImap = JUserImapSettings.USER_IMAP_SETTINGS
            DbConnection.use {
                DSL.using(it)
                    .select(userImap)
                    .from(userImap)
                    .where(userImap.USER_ID.eq(userId.id))
                    .fetchOne()
            }
        }.fold(
            onSuccess = { record1 ->
                val imapConfig = record1?.value1() ?: return ImapConfig(
                    host = null,
                    port = null,
                    userName = null,
                    password = null,
                )

                ImapConfig(
                    host = imapConfig.host,
                    port = imapConfig.port,
                    userName = imapConfig.useName,
                    password = imapConfig.password,
                )
            },
            onFailure = {
                it.printStackTrace()
                null
            },
        )
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
