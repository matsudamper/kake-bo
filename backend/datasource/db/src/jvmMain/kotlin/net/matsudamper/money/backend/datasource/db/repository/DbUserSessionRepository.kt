package net.matsudamper.money.backend.datasource.db.repository

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JUserSessions
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbUserSessionRepository : UserSessionRepository {
    private val userSessions = JUserSessions.USER_SESSIONS

    override fun clearSession(sessionId: UserSessionId) {
        DbConnectionImpl.use {
            DSL.using(it)
                .deleteFrom(userSessions)
                .where(userSessions.SESSION_ID.eq(sessionId.id))
                .execute()
        }
    }

    override fun createSession(userId: UserId): UserSessionRepository.CreateSessionResult {
        val result = DbConnectionImpl.use {
            DSL.using(it)
                .insertInto(userSessions)
                .set(userSessions.USER_ID, userId.value)
                .set(userSessions.SESSION_ID, UUID.randomUUID().toString().replace("-", ""))
                .returningResult(userSessions.SESSION_ID, userSessions.LATEST_ACCESSED_AT)
                .fetchOne()
        }

        return UserSessionRepository.CreateSessionResult(
            sessionId = UserSessionId(result!!.value1()!!),
            latestAccess = result.get<LocalDateTime>(userSessions.LATEST_ACCESSED_AT),
        )
    }

    override fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): UserSessionRepository.VerifySessionResult {
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
                    DSL.localDateTimeAdd(userSessions.LATEST_ACCESSED_AT, expireDay)
                        .lessThan(LocalDateTime.now(ZoneOffset.UTC)),
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
                    .set(userSessions.LATEST_ACCESSED_AT, LocalDateTime.now(ZoneOffset.UTC))
                    .where(userSessions.SESSION_ID.eq(sessionId.id))
                    .returningResult(userSessions.USER_ID, userSessions.SESSION_ID, userSessions.LATEST_ACCESSED_AT)
                    .fetchOne()
            }
        }

        val userId = result?.value1() ?: return UserSessionRepository.VerifySessionResult.Failure

        return UserSessionRepository.VerifySessionResult.Success(
            userId = UserId(userId),
            sessionId = UserSessionId(result.get<String>(userSessions.SESSION_ID)),
            latestAccess = result.get<LocalDateTime>(userSessions.LATEST_ACCESSED_AT),
        )
    }

    override fun getSessionInfo(sessionId: UserSessionId): UserSessionRepository.SessionInfo? {
        val result = DbConnectionImpl.use {
            DSL.using(it)
                .select(
                    userSessions.LATEST_ACCESSED_AT,
                    userSessions.NAME,
                )
                .from(userSessions)
                .where(userSessions.SESSION_ID.eq(sessionId.id))
                .fetchOne()
        } ?: return null

        return UserSessionRepository.SessionInfo(
            latestAccess = result.get<LocalDateTime>(userSessions.LATEST_ACCESSED_AT),
            name = result.get<String>(userSessions.NAME),
        )
    }

    override fun getSessions(userId: UserId): List<UserSessionRepository.SessionInfo> {
        return DbConnectionImpl.use {
            DSL.using(it)
                .select(
                    userSessions.SESSION_ID,
                    userSessions.LATEST_ACCESSED_AT,
                    userSessions.NAME,
                )
                .from(userSessions)
                .where(userSessions.USER_ID.eq(userId.value))
                .fetch()
                .map { record ->
                    UserSessionRepository.SessionInfo(
                        latestAccess = record.get<LocalDateTime>(userSessions.LATEST_ACCESSED_AT),
                        name = record.get<String>(userSessions.NAME),
                    )
                }
        }
    }

    override fun deleteSession(
        userId: UserId,
        sessionName: String,
        currentSessionName: String,
    ): Boolean {
        return DbConnectionImpl.use {
            DSL.using(it)
                .deleteFrom(userSessions)
                .where(
                    userSessions.USER_ID.eq(userId.value),
                    userSessions.NAME.eq(sessionName),
                    userSessions.NAME.notEqual(currentSessionName),
                )
                .limit(1)
                .execute() > 0
        }
    }

    override fun changeSessionName(
        sessionId: UserSessionId,
        name: String,
    ): UserSessionRepository.SessionInfo? {
        val result = DbConnectionImpl.use {
            DSL.using(it)
                .update(userSessions)
                .set(userSessions.NAME, name)
                .where(userSessions.SESSION_ID.eq(sessionId.id))
                .limit(1)
                .returningResult(userSessions.LATEST_ACCESSED_AT)
                .fetchOne()
        } ?: return null

        return UserSessionRepository.SessionInfo(
            latestAccess = result.get<LocalDateTime>(userSessions.LATEST_ACCESSED_AT),
            name = name,
        )
    }
}
