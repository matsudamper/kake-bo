package net.matsudamper.money.backend.app.interfaces

import java.time.Instant
import java.time.LocalDateTime
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.element.SessionRecordId
import net.matsudamper.money.element.UserId

interface UserSessionRepository {
    fun createSession(userId: UserId): CreateSessionResult

    fun clearSession(sessionId: UserSessionId)

    fun changeSessionName(
        sessionRecordId: SessionRecordId,
        sessionName: String,
    ): SessionInfo?

    fun deleteSession(
        currentSessionId: UserSessionId,
        targetSessionRecordId: SessionRecordId,
    ): Boolean

    fun getSessions(userId: UserId): List<SessionInfo>

    fun getSessionInfo(sessionId: UserSessionId): SessionInfo?

    fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): VerifySessionResult

    data class CreateSessionResult(
        val sessionId: UserSessionId,
        val latestAccess: LocalDateTime,
    )

    data class SessionInfo(
        val sessionRecordId: SessionRecordId,
        val name: String,
        val latestAccess: Instant,
    )

    sealed interface VerifySessionResult {
        data class Success(
            val userId: UserId,
            val sessionId: UserSessionId,
            val latestAccess: LocalDateTime,
        ) : VerifySessionResult

        data object Failure : VerifySessionResult
    }
}
