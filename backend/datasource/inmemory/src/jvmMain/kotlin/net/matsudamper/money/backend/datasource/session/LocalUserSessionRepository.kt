package net.matsudamper.money.backend.datasource.session

import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.element.UserId

internal class LocalUserSessionRepository(
    private val clock: Clock,
) : UserSessionRepository {

    private val sessions = ConcurrentHashMap<UserSessionId, SessionData>()
    private val userSessions = ConcurrentHashMap<UserId, MutableSet<UserSessionId>>()
    private val userSessionNames = ConcurrentHashMap<Pair<UserId, String>, UserSessionId>()

    override fun clearSession(sessionId: UserSessionId) {
        val sessionData = sessions.remove(sessionId) ?: return

        val userId = UserId(sessionData.userId)
        userSessions[userId]?.remove(sessionId)
        if (userSessions[userId]?.isEmpty() == true) {
            userSessions.remove(userId)
        }

        if (sessionData.sessionName.isNotEmpty()) {
            userSessionNames.remove(userId to sessionData.sessionName)
        }
    }

    override fun createSession(userId: UserId): UserSessionRepository.CreateSessionResult {
        val sessionId = UserSessionId(UUID.randomUUID().toString().replace("-", ""))
        val now = LocalDateTime.now(clock)

        val sessionData = SessionData(
            userId = userId.value,
            latestAccess = now.toString(),
            sessionId = sessionId.id,
        )
        sessions[sessionId] = sessionData

        userSessions.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(sessionId)

        return UserSessionRepository.CreateSessionResult(
            sessionId = sessionId,
            latestAccess = now,
        )
    }

    override fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): UserSessionRepository.VerifySessionResult {
        val sessionData = sessions[sessionId] ?: return UserSessionRepository.VerifySessionResult.Failure

        val now = LocalDateTime.now(clock)
        sessions[sessionId] = sessionData.copy(latestAccess = now.toString())

        return UserSessionRepository.VerifySessionResult.Success(
            userId = UserId(sessionData.userId),
            sessionId = sessionId,
            latestAccess = now,
        )
    }

    override fun getSessionInfo(sessionId: UserSessionId): UserSessionRepository.SessionInfo? {
        val sessionData = sessions[sessionId] ?: return null
        val latestAccess = try {
            LocalDateTime.parse(sessionData.latestAccess)
        } catch (e: Throwable) {
            TraceLogger.impl().noticeThrowable(e, true)
            TraceLogger.impl().setAttribute("jsonData", sessionData.toString())
            LocalDateTime.now(clock)
        }
        return UserSessionRepository.SessionInfo(
            sessionId = sessionId,
            name = sessionData.sessionName,
            latestAccess = latestAccess,
        )
    }

    override fun getSessions(userId: UserId): List<UserSessionRepository.SessionInfo> {
        val sessionIds = userSessions[userId] ?: return emptyList()

        return sessionIds.mapNotNull { sessionId ->
            sessions[sessionId]?.let { data ->
                val latestAccess = try {
                    LocalDateTime.parse(data.latestAccess)
                } catch (e: Throwable) {
                    TraceLogger.impl().noticeThrowable(e, true)
                    TraceLogger.impl().setAttribute("latestAccess", data.latestAccess)
                    LocalDateTime.now(clock)
                }
                UserSessionRepository.SessionInfo(
                    sessionId = sessionId,
                    name = data.sessionName,
                    latestAccess = latestAccess,
                )
            }
        }
    }

    override fun deleteSession(
        userId: UserId,
        sessionId: UserSessionId,
        currentSessionId: UserSessionId,
    ): Boolean {
        if (sessionId == currentSessionId) {
            return false
        }

        val userSessionIds = userSessions[userId] ?: return false
        if (!userSessionIds.contains(sessionId)) {
            return false
        }

        clearSession(sessionId)
        return true
    }

    override fun changeSessionName(
        sessionId: UserSessionId,
        name: String,
    ): UserSessionRepository.SessionInfo? {
        val sessionData = sessions[sessionId] ?: return null

        val userId = UserId(sessionData.userId)

        if (sessionData.sessionName.isNotEmpty()) {
            userSessionNames.remove(userId to sessionData.sessionName)
        }

        userSessionNames[userId to name] = sessionId

        val updatedSessionData = sessionData.copy(sessionName = name)
        sessions[sessionId] = updatedSessionData

        val latestAccess = try {
            LocalDateTime.parse(updatedSessionData.latestAccess)
        } catch (e: Throwable) {
            TraceLogger.impl().noticeThrowable(e, true)
            TraceLogger.impl().setAttribute("latestAccess", updatedSessionData.latestAccess)
            LocalDateTime.now(clock)
        }

        return UserSessionRepository.SessionInfo(
            sessionId = sessionId,
            name = name,
            latestAccess = latestAccess,
        )
    }

    private data class SessionData(
        val userId: Int,
        val sessionName: String = "",
        val latestAccess: String,
        val sessionId: String,
    )
}
