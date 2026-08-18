package net.matsudamper.money.backend.datasource.session

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.element.SessionRecordId
import net.matsudamper.money.element.UserId

internal class LocalUserSessionRepository(
    private val clock: Clock,
) : UserSessionRepository {

    private val sessions = ConcurrentHashMap<UserSessionId, SessionData>()
    private val sessionRecords = ConcurrentHashMap<SessionRecordId, UserSessionId>()
    private val userSessions = ConcurrentHashMap<UserId, MutableSet<SessionRecordId>>()

    override fun clearSession(sessionId: UserSessionId) {
        val sessionData = sessions.remove(sessionId) ?: return

        // 空判定と削除の間にcreateSession()が追加すると新しいセッションごとSetが消えるためアトミックに更新する
        userSessions.computeIfPresent(sessionData.userId) { _, recordIds ->
            recordIds.remove(sessionData.sessionRecordId)
            if (recordIds.isEmpty()) {
                null
            } else {
                recordIds
            }
        }
        sessionRecords.remove(sessionData.sessionRecordId)
    }

    override fun createSession(userId: UserId): UserSessionRepository.CreateSessionResult {
        val sessionId = UserSessionId(UUID.randomUUID().toString().replace("-", ""))
        val sessionRecordId = SessionRecordId(UUID.randomUUID().toString())
        val now = Instant.now(clock)

        sessions[sessionId] = SessionData(
            userId = userId,
            sessionRecordId = sessionRecordId,
            createdAt = now,
            lastAccess = now,
            name = UUID.randomUUID().toString().replace("-", ""),
        )
        sessionRecords[sessionRecordId] = sessionId
        userSessions.compute(userId) { _, recordIds ->
            val newRecordIds = recordIds ?: ConcurrentHashMap.newKeySet()
            newRecordIds.add(sessionRecordId)
            newRecordIds
        }

        return UserSessionRepository.CreateSessionResult(
            sessionId = sessionId,
            latestAccess = LocalDateTime.now(clock),
        )
    }

    override fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): UserSessionRepository.VerifySessionResult {
        val now = Instant.now(clock)
        val expireThreshold = now.minus(expireDay, ChronoUnit.DAYS)

        // 読み取りと書き込みの間にclearSession()が走ると削除済みセッションが復活するためアトミックに更新する
        val sessionData = sessions.computeIfPresent(sessionId) { _, current ->
            if (current.lastAccess.isBefore(expireThreshold)) {
                current
            } else {
                current.copy(lastAccess = now)
            }
        } ?: return UserSessionRepository.VerifySessionResult.Failure

        if (sessionData.lastAccess.isBefore(expireThreshold)) {
            clearSession(sessionId)
            return UserSessionRepository.VerifySessionResult.Failure
        }

        return UserSessionRepository.VerifySessionResult.Success(
            userId = sessionData.userId,
            sessionId = sessionId,
            latestAccess = LocalDateTime.now(clock),
        )
    }

    override fun getSessionInfo(sessionId: UserSessionId): UserSessionRepository.SessionInfo? {
        val sessionData = sessions[sessionId] ?: return null
        return UserSessionRepository.SessionInfo(
            sessionRecordId = sessionData.sessionRecordId,
            name = sessionData.name,
            latestAccess = sessionData.lastAccess,
        )
    }

    override fun getSessions(userId: UserId): List<UserSessionRepository.SessionInfo> {
        val recordIds = userSessions[userId] ?: return listOf()

        return recordIds.mapNotNull { recordId ->
            val sessionId = sessionRecords[recordId] ?: return@mapNotNull null
            val sessionData = sessions[sessionId] ?: return@mapNotNull null
            UserSessionRepository.SessionInfo(
                sessionRecordId = recordId,
                name = sessionData.name,
                latestAccess = sessionData.lastAccess,
            )
        }
    }

    override fun deleteSession(
        currentSessionId: UserSessionId,
        targetSessionRecordId: SessionRecordId,
    ): Boolean {
        val currentUserId = sessions[currentSessionId]?.userId ?: return false
        val sessionId = sessionRecords[targetSessionRecordId] ?: return false
        val targetSessionData = sessions[sessionId] ?: return false
        if (currentUserId != targetSessionData.userId) return false

        clearSession(sessionId)
        return true
    }

    override fun changeSessionName(
        currentSessionId: UserSessionId,
        sessionRecordId: SessionRecordId,
        sessionName: String,
    ): UserSessionRepository.SessionInfo? {
        val currentUserId = sessions[currentSessionId]?.userId ?: return null
        val sessionId = sessionRecords[sessionRecordId] ?: return null
        val sessionData = sessions.computeIfPresent(sessionId) { _, current ->
            if (current.userId == currentUserId) {
                current.copy(name = sessionName)
            } else {
                current
            }
        } ?: return null
        if (sessionData.userId != currentUserId) return null

        return UserSessionRepository.SessionInfo(
            sessionRecordId = sessionRecordId,
            name = sessionData.name,
            latestAccess = sessionData.lastAccess,
        )
    }

    // プロセス内のみで完結するため事前に確立する接続を持たない
    override fun warmup() = Unit

    private data class SessionData(
        val userId: UserId,
        val sessionRecordId: SessionRecordId,
        val createdAt: Instant,
        val lastAccess: Instant,
        val name: String,
    )
}
