package net.matsudamper.money.backend.datasource.session

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
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

        userSessions[sessionData.userId]?.remove(sessionData.sessionRecordId)
        if (userSessions[sessionData.userId]?.isEmpty() == true) {
            userSessions.remove(sessionData.userId)
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
        userSessions.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(sessionRecordId)

        return UserSessionRepository.CreateSessionResult(
            sessionId = sessionId,
            latestAccess = LocalDateTime.now(clock),
        )
    }

    override fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): UserSessionRepository.VerifySessionResult {
        val sessionData = sessions[sessionId] ?: return UserSessionRepository.VerifySessionResult.Failure

        sessions[sessionId] = sessionData.copy(lastAccess = Instant.now(clock))

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
        val sessionData = sessions[sessionId] ?: return null
        if (sessionData.userId != currentUserId) return null

        sessions[sessionId] = sessionData.copy(name = sessionName)

        return UserSessionRepository.SessionInfo(
            sessionRecordId = sessionRecordId,
            name = sessionName,
            latestAccess = sessionData.lastAccess,
        )
    }

    private data class SessionData(
        val userId: UserId,
        val sessionRecordId: SessionRecordId,
        val createdAt: Instant,
        val lastAccess: Instant,
        val name: String,
    )
}
