package net.matsudamper.money.backend.datasource.db.repository

import java.util.UUID
import net.matsudamper.money.backend.app.interfaces.AdminSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.AdminSession
import net.matsudamper.money.backend.app.interfaces.element.AdminSessionId
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.backend.datasource.db.dsl.LocalDateTimeExt
import net.matsudamper.money.db.schema.tables.JAdminSessions
import net.matsudamper.money.db.schema.tables.records.JAdminSessionsRecord
import org.jooq.impl.DSL

/**
 * ＝＝	eq	左項の値が右項の値に等しいとき真になる
 * ＞	gt	左項の値が右項の値より大きいときに真になる
 * ＞＝	ge	左項の値が右項の値以上のときに真になる
 * ＜＝	le	左項の値が右項の値以下のとき真になる
 * ＜	lt	左項の値が右項の値より小さいときに真になる
 * ！＝	ne  左項の値が右項の値に等しくないときに真になる
 */
class DbAdminSessionRepository(
    private val dbConnection: DbConnection,
) : AdminSessionRepository {
    private val adminSession = JAdminSessions.ADMIN_SESSIONS

    /**
     * 期限は最終使用から10分
     * @return true: 有効なSessionID
     */
    override fun verifySession(adminSessionId: String): AdminSession? {
        // 期限切れのSessionを削除する
        dbConnection.use {
            DSL.using(it)
                .deleteFrom(adminSession)
                .where(
                    adminSession.EXPIRE_DATETIME
                        .lt(DSL.localDateTime(LocalDateTimeExt.nowUtc())),
                )
                .execute()
        }

        dbConnection.use {
            // verify
            DSL.using(it)
                .select()
                .from(adminSession)
                .where(
                    adminSession.SESSION_ID.eq(adminSessionId)
                        .and(
                            adminSession.EXPIRE_DATETIME
                                .gt(DSL.localDateTime(LocalDateTimeExt.nowUtc())),
                        ),
                )
                .fetchAny()
        } ?: return null

        val sessionId =
            dbConnection.use {
                // update Expire
                DSL.using(it)
                    .update(adminSession)
                    .set(
                        adminSession.EXPIRE_DATETIME,
                        DSL.localDateTime(
                            LocalDateTimeExt.nowUtc()
                                .plusMinutes(10),
                        ),
                    )
                    .where(
                        adminSession.SESSION_ID.eq(adminSessionId),
                    )
                    .returningResult(adminSession)
                    .fetchOne()!!
                    .value1()!!
            }
        return sessionId.toResponse()
    }

    override fun createSession(): AdminSession {
        val sessionId =
            dbConnection.use {
                DSL.using(it)
                    .insertInto(adminSession)
                    .set(
                        adminSession.EXPIRE_DATETIME,
                        DSL.localDateTime(LocalDateTimeExt.nowUtc().plusMinutes(10)),
                    )
                    .set(adminSession.SESSION_ID, UUID.randomUUID().toString())
                    .returningResult(adminSession)
                    .fetchOne()!!
                    .value1()!!
            }

        return sessionId.toResponse()
    }

    private fun JAdminSessionsRecord.toResponse(): AdminSession {
        return AdminSession(
            adminSessionId = AdminSessionId(sessionId!!),
            expire = expireDatetime!!,
        )
    }
}
