package net.matsudamper.money.backend.datasource.session

import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlinx.serialization.Serializable
import io.lettuce.core.ClientOptions
import io.lettuce.core.MaintNotificationsConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.resource.ClientResources
import io.opentelemetry.instrumentation.lettuce.v5_1.LettuceTelemetry
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.backend.base.ObjectMapper
import net.matsudamper.money.backend.base.OpenTelemetryInitializer
import net.matsudamper.money.backend.base.ServerVariables
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.element.UserId

internal class RedisUserSessionRepository(
    host: String,
    port: Int,
    index: Int,
    private val clock: Clock,
) : UserSessionRepository {
    private val redisClient: RedisClient = run {
        val uri = RedisURI.Builder.redis(host, port).withDatabase(index).build()
        val clientResources = ClientResources.builder()
            .tracing(LettuceTelemetry.create(OpenTelemetryInitializer.get()).createTracing())
            .build()
        RedisClient.create(clientResources, uri).apply {
            setOptions(
                ClientOptions.builder()
                    .maintNotificationsConfig(MaintNotificationsConfig.disabled())
                    .build(),
            )
        }
    }
    private val connection: StatefulRedisConnection<String, String> by lazy { redisClient.connect() }
    private val commands by lazy { connection.sync() }

    override fun clearSession(sessionId: UserSessionId) {
        val sessionKey = SessionKeys.Session(sessionId).key
        val jsonData = commands.get(sessionKey)

        if (jsonData != null) {
            val sessionData = try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("jsonData", jsonData)
                null
            }

            if (sessionData != null) {
                val userId = UserId(sessionData.userId)
                val sessionName = sessionData.sessionName

                commands.srem(SessionKeys.UserSessions(userId).key, sessionId.id)

                if (sessionName.isNotEmpty()) {
                    commands.del(SessionKeys.UserSessionByUser(userId, sessionName).key)
                }
            }
        }

        commands.del(sessionKey)
    }

    override fun createSession(userId: UserId): UserSessionRepository.CreateSessionResult {
        val sessionId = UserSessionId(UUID.randomUUID().toString().replace("-", ""))
        val now = LocalDateTime.now(clock)

        val sessionData = SessionData(
            userId = userId.value,
            latestAccess = now.toString(),
            sessionName = UUID.randomUUID().toString().replace("-", ""),
        )

        val jsonData = ObjectMapper.kotlinxSerialization.encodeToString(sessionData)

        commands.set(
            SessionKeys.Session(sessionId).key,
            jsonData,
            SetArgs().ex(ServerVariables.USER_SESSION_EXPIRE_DAY.days.inWholeSeconds),
        )

        val userSessionsKey = SessionKeys.UserSessions(userId).key
        commands.sadd(userSessionsKey, sessionId.id)
        commands.expire(userSessionsKey, ServerVariables.USER_SESSION_EXPIRE_DAY.days.inWholeSeconds)

        return UserSessionRepository.CreateSessionResult(
            sessionId = sessionId,
            latestAccess = now,
        )
    }

    override fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): UserSessionRepository.VerifySessionResult {
        val now = LocalDateTime.now(clock)

        val sessionKey = SessionKeys.Session(sessionId).key
        val jsonData = commands.get(sessionKey) ?: return UserSessionRepository.VerifySessionResult.Failure

        val sessionData = try {
            ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
        } catch (e: Throwable) {
            TraceLogger.impl().noticeThrowable(e, true)
            TraceLogger.impl().setAttribute("jsonData", jsonData)
            return UserSessionRepository.VerifySessionResult.Failure
        }

        val userId = UserId(sessionData.userId)
        val sessionName = sessionData.sessionName

        commands.expire(sessionKey, ServerVariables.USER_SESSION_EXPIRE_DAY.days.inWholeSeconds)
        commands.expire(SessionKeys.UserSessions(userId).key, ServerVariables.USER_SESSION_EXPIRE_DAY.days.inWholeSeconds)

        if (sessionName.isNotEmpty()) {
            commands.expire(SessionKeys.UserSessionByUser(userId, sessionName).key, ServerVariables.USER_SESSION_EXPIRE_DAY.days.inWholeSeconds)
        }

        return UserSessionRepository.VerifySessionResult.Success(
            userId = userId,
            sessionId = sessionId,
            latestAccess = now,
        )
    }

    override fun getSessionInfo(sessionId: UserSessionId): UserSessionRepository.SessionInfo? {
        val sessionKey = SessionKeys.Session(sessionId).key
        val jsonData = commands.get(sessionKey) ?: return null

        val sessionData = try {
            ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
        } catch (e: Throwable) {
            TraceLogger.impl().noticeThrowable(e, true)
            TraceLogger.impl().setAttribute("jsonData", jsonData)
            return null
        }

        val latestAccess = try {
            LocalDateTime.parse(sessionData.latestAccess)
        } catch (e: Throwable) {
            TraceLogger.impl().noticeThrowable(e, true)
            TraceLogger.impl().setAttribute("latestAccess", sessionData.latestAccess)
            LocalDateTime.now(clock)
        }

        return UserSessionRepository.SessionInfo(
            name = sessionData.sessionName,
            latestAccess = latestAccess,
        )
    }

    override fun getSessions(userId: UserId): List<UserSessionRepository.SessionInfo> {
        val sessionsKey = SessionKeys.UserSessions(userId).key
        val sessionIds = commands.smembers(sessionsKey)

        return sessionIds.mapNotNull { sessionIdStr ->
            val sessionKey = SessionKeys.Session(UserSessionId(sessionIdStr)).key
            val jsonData = commands.get(sessionKey) ?: return@mapNotNull null

            val sessionData = try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("jsonData", jsonData)
                return@mapNotNull null
            }

            val latestAccess = try {
                LocalDateTime.parse(sessionData.latestAccess)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("latestAccess", sessionData.latestAccess)
                LocalDateTime.now(clock)
            }

            UserSessionRepository.SessionInfo(
                name = sessionData.sessionName,
                latestAccess = latestAccess,
            )
        }
    }

    override fun deleteSession(
        userId: UserId,
        sessionName: String,
        currentSessionName: String,
    ): Boolean {
        if (sessionName == currentSessionName) {
            return false
        }

        val userSessionKey = SessionKeys.UserSessionByUser(userId, sessionName).key
        val sessionIdStr = commands.get(userSessionKey) ?: return false

        val sessionId = UserSessionId(sessionIdStr)
        clearSession(sessionId)

        return true
    }

    override fun changeSessionName(
        sessionId: UserSessionId,
        name: String,
    ): UserSessionRepository.SessionInfo? {
        val sessionKey = SessionKeys.Session(sessionId).key
        val jsonData = commands.get(sessionKey) ?: return null

        val sessionData = try {
            ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
        } catch (e: Throwable) {
            TraceLogger.impl().noticeThrowable(e, true)
            TraceLogger.impl().setAttribute("jsonData", jsonData)
            return null
        }

        val userId = UserId(sessionData.userId)
        val oldName = sessionData.sessionName
        val latestAccess = try {
            LocalDateTime.parse(sessionData.latestAccess)
        } catch (e: Throwable) {
            TraceLogger.impl().noticeThrowable(e, true)
            TraceLogger.impl().setAttribute("latestAccess", sessionData.latestAccess)
            LocalDateTime.now(clock)
        }

        if (oldName.isNotEmpty()) {
            commands.del(SessionKeys.UserSessionByUser(userId, oldName).key)
        }

        commands.set(SessionKeys.UserSessionByUser(userId, name).key, sessionId.id)

        val updatedSessionData = sessionData.copy(sessionName = name)
        val updatedJsonData = ObjectMapper.kotlinxSerialization.encodeToString(updatedSessionData)
        commands.set(sessionKey, updatedJsonData)

        return UserSessionRepository.SessionInfo(
            name = name,
            latestAccess = latestAccess,
        )
    }

    sealed interface SessionKeys {
        val key: String

        class Session(sessionId: UserSessionId) : SessionKeys {
            override val key: String = "user_session:${sessionId.id}"
        }

        class UserSessionByUser(userId: UserId, sessionName: String) : SessionKeys {
            override val key: String = "user_session_by_user:${userId.value}:$sessionName"
        }

        class UserSessions(userId: UserId) : SessionKeys {
            override val key: String = "user_sessions:${userId.value}"
        }
    }

    @Serializable
    private data class SessionData(
        val userId: Int,
        val sessionName: String,
        val latestAccess: String,
    )
}
