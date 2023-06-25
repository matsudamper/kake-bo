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
    fun updateImapConfig(userId: UserId, host: String?, port: Int?, password: String?, userName: String?): Boolean {
        return runCatching {
            val userImap = JUserImapSettings.USER_IMAP_SETTINGS
            DSL.using(DbConnection.get())
                .insertInto(userImap)
                .set(userImap.USER_ID, userId.id)
                .onDuplicateKeyUpdate()
                .set(userImap.HOST, host)
                .set(userImap.PORT, port)
                .set(userImap.PASSWORD, password)
                .set(userImap.USE_NAME, userName)
                .execute()
        }.fold(
            onSuccess = { true },
            onFailure = { false },
        )
    }

    fun getImapConfig(userId: UserId): ImapConfig? {
        return runCatching {
            val userImap = JUserImapSettings.USER_IMAP_SETTINGS
            DSL.using(DbConnection.get())
                .select(userImap)
                .where(userImap.USER_ID.eq(userId.id))
                .fetchOne()
        }.fold(
            onSuccess = { record1 ->
                val imapConfig = record1?.value1() ?: return null

                ImapConfig(
                    host = imapConfig.host,
                    port = imapConfig.port,
                    userName = imapConfig.useName,
                    password = imapConfig.password,
                )
            },
            onFailure = { null },
        )
    }
}
