package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.UserConfigRepository
import net.matsudamper.money.backend.app.interfaces.UserConfigRepository.Companion.TIMEZONE_OFFSET_RANGE
import net.matsudamper.money.backend.app.interfaces.element.ImapConfig
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JUserImapSettings
import net.matsudamper.money.db.schema.tables.JUserSetting
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbUserConfigRepository : UserConfigRepository {
    /**
     * @return isSuccess
     */
    override fun updateImapConfig(
        userId: UserId,
        host: String?,
        port: Int?,
        password: String?,
        userName: String?,
    ): Boolean {
        return runCatching {
            val userImap = JUserImapSettings.USER_IMAP_SETTINGS
            DbConnectionImpl.use {
                host?.also { host ->
                    DSL.using(it)
                        .insertInto(userImap)
                        .set(userImap.USER_ID, userId.value)
                        .onDuplicateKeyUpdate()
                        .set(userImap.HOST, host)
                        .execute()
                }
                port?.also { port ->
                    DSL.using(it)
                        .insertInto(userImap)
                        .set(userImap.USER_ID, userId.value)
                        .onDuplicateKeyUpdate()
                        .set(userImap.PORT, port)
                        .execute()
                }
                password?.also { password ->
                    DSL.using(it)
                        .insertInto(userImap)
                        .set(userImap.USER_ID, userId.value)
                        .onDuplicateKeyUpdate()
                        .set(userImap.PASSWORD, password)
                        .execute()
                }
                userName?.also { userName ->
                    DSL.using(it)
                        .insertInto(userImap)
                        .set(userImap.USER_ID, userId.value)
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

    override fun getTimezoneOffset(userId: UserId): Int {
        return runCatching {
            val userSetting = JUserSetting.USER_SETTING
            DbConnectionImpl.use {
                DSL.using(it)
                    .select(userSetting.TIMEZONE_OFFSET_MINUTES)
                    .from(userSetting)
                    .where(userSetting.USER_ID.eq(userId.value))
                    .fetchOne()
            }
        }.fold(
            onSuccess = { record -> record?.value1() ?: 0 },
            onFailure = { throw it },
        )
    }

    override fun updateTimezoneOffset(userId: UserId, offsetMinutes: Int): Boolean {
        if (offsetMinutes !in TIMEZONE_OFFSET_RANGE) {
            return false
        }
        return runCatching {
            val userSetting = JUserSetting.USER_SETTING
            DbConnectionImpl.use {
                DSL.using(it)
                    .insertInto(userSetting)
                    .set(userSetting.USER_ID, userId.value)
                    .set(userSetting.TIMEZONE_OFFSET_MINUTES, offsetMinutes)
                    .onDuplicateKeyUpdate()
                    .set(userSetting.TIMEZONE_OFFSET_MINUTES, offsetMinutes)
                    .execute()
            }
        }.fold(
            onSuccess = { true },
            onFailure = { false },
        )
    }

    override fun getImapConfig(userId: UserId): ImapConfig? {
        return runCatching {
            val userImap = JUserImapSettings.USER_IMAP_SETTINGS
            DbConnectionImpl.use {
                DSL.using(it)
                    .select(userImap)
                    .from(userImap)
                    .where(userImap.USER_ID.eq(userId.value))
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
}
