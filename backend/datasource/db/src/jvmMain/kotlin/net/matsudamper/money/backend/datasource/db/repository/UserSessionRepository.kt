package net.matsudamper.money.backend.datasource.db.repository

import java.time.LocalDateTime
import java.util.*
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.backend.datasource.db.element.UserSessionId
import net.matsudamper.money.db.schema.tables.JUserSessions
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class UserSessionRepository {
    private val userSessions = JUserSessions.USER_SESSIONS

    fun clearSession(sessionId: UserSessionId) {
        DbConnectionImpl.use {
            DSL.using(it)
                .deleteFrom(userSessions)
                .where(userSessions.SESSION_ID.eq(sessionId.id))
                .execute()
        }
    }

    fun createSession(userId: UserId): CreateSessionResult {
        val result = DbConnectionImpl.use {
            DSL.using(it)
                .insertInto(userSessions)
                .set(userSessions.USER_ID, userId.value)
                .set(userSessions.SESSION_ID, UUID.randomUUID().toString().replace("-", ""))
                .set(userSessions.EXPIRE_DATETIME, getNewExpire())
                .returningResult(userSessions.SESSION_ID, userSessions.EXPIRE_DATETIME)
                .fetchOne()
        }

        return CreateSessionResult(
            sessionId = UserSessionId(result!!.value1()!!),
            expire = result.value2()!!,
        )
    }

    fun verifySession(sessionId: UserSessionId): VerifySessionResult {
        // UserIdを取得し、全ての古いSessionを削除する
        DbConnectionImpl.use {
            val userId = DSL.using(it)
                .select(userSessions.USER_ID)
                .from(userSessions)
                .where(userSessions.SESSION_ID.eq(sessionId.id))
                .execute()

            DSL.using(it)
                .deleteFrom(userSessions)
                .where(
                    userSessions.USER_ID.eq(userId),
                    userSessions.EXPIRE_DATETIME.lt(LocalDateTime.now()),
                )
                .execute()
        }

        // Sessionを更新する
        val result = DbConnectionImpl.use {
            DSL.using(it).transactionResult { config ->
                DSL.using(config)
                    .select(userSessions.SESSION_ID)
                    .from(userSessions)
                    .where(userSessions.SESSION_ID.eq(sessionId.id))
                    .forUpdate()
                    .execute()

                DSL.using(config)
                    .update(userSessions)
                    .set(userSessions.EXPIRE_DATETIME, getNewExpire())
                    .where(userSessions.SESSION_ID.eq(sessionId.id))
                    .returningResult(userSessions.USER_ID, userSessions.SESSION_ID, userSessions.EXPIRE_DATETIME)
                    .fetchOne()
            }
        }

        val userId = result?.value1() ?: return VerifySessionResult.Failure

        return VerifySessionResult.Success(
            userId = UserId(userId),
            sessionId = UserSessionId(result.value2()!!),
            expire = result.value3()!!,
        )
    }

    private fun getNewExpire() = LocalDateTime.now().plusDays(7)

    data class CreateSessionResult(
        val sessionId: UserSessionId,
        val expire: LocalDateTime,
    )

    sealed interface VerifySessionResult {
        data class Success(
            val userId: UserId,
            val sessionId: UserSessionId,
            val expire: LocalDateTime,
        ) : VerifySessionResult

        data object Failure : VerifySessionResult
    }
}
