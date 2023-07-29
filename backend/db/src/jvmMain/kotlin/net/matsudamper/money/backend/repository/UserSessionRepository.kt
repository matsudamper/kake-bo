package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.element.UserSession
import net.matsudamper.money.db.schema.tables.JUserSessions
import org.jooq.impl.DSL
import java.time.LocalDateTime
import java.util.*

class UserSessionRepository {
    private val userSessions = JUserSessions.USER_SESSIONS

    fun createSession(userId: UserId): CreateSessionResult {
        val result = DbConnection.use {
            DSL.using(it)
                .insertInto(userSessions)
                .set(userSessions.USER_ID, userId.id)
                .set(userSessions.SESSION_ID, UUID.randomUUID().toString().replace("-", ""))
                .set(userSessions.EXPIRE_DATETIME, getNewExpire())
                .returningResult(userSessions.SESSION_ID, userSessions.EXPIRE_DATETIME)
                .fetchOne()
        }

        return CreateSessionResult(
            sessionId = UserSession(result!!.value1()!!),
            expire = result.value2()!!,
        )
    }

    fun verifySession(sessionId: UserSession): VerifySessionResult {
        // UserIdを取得し、全ての古いSessionを削除する
        DbConnection.use {
            val userId = DSL.select(userSessions.USER_ID)
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
        val result = DbConnection.use {
            DSL.using(it)
                .update(userSessions)
                .set(userSessions.EXPIRE_DATETIME, getNewExpire())
                .where(userSessions.SESSION_ID.eq(sessionId.id))
                .returningResult(userSessions.USER_ID, userSessions.SESSION_ID, userSessions.EXPIRE_DATETIME)
                .fetchOne()
        }

        val userId = result?.value1() ?: return VerifySessionResult.Failure

        return VerifySessionResult.Success(
            userId = UserId(userId),
            sessionId = UserSession(result.value2()!!),
            expire = result.value3()!!,
        )
    }

    private fun getNewExpire() = LocalDateTime.now().plusDays(7)

    data class CreateSessionResult(
        val sessionId: UserSession,
        val expire: LocalDateTime,
    )

    sealed interface VerifySessionResult {
        data class Success(
            val userId: UserId,
            val sessionId: UserSession,
            val expire: LocalDateTime,
        ) : VerifySessionResult

        object Failure : VerifySessionResult
    }
}