package net.matsudamper.money.backend.datasource.session

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
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
) : UserSessionRepository {
    private val tracer = OpenTelemetryInitializer.get().getTracer("redis.lettuce")
    private val redisClient: RedisClient = run {
        val uri = RedisURI.Builder.redis(host, port).withDatabase(index).build()
        RedisClient.create(uri)
    }
    private val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val commands = connection.sync()

    override fun clearSession(sessionId: UserSessionId) {
        redisSpan("clearSession") {
            val sessionKey = getSessionKey(sessionId)
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

                    commands.srem(getUserSessionsKey(userId), sessionId.id)

                    if (sessionName.isNotEmpty()) {
                        commands.del(getUserSessionKey(userId, sessionName))
                    }
                }
            }

            commands.del(sessionKey)
        }
    }

    override fun createSession(userId: UserId): UserSessionRepository.CreateSessionResult {
        return redisSpan("createSession") {
            val sessionId = UserSessionId(UUID.randomUUID().toString().replace("-", ""))
            val now = LocalDateTime.now(ZoneOffset.UTC)

            val sessionData = SessionData(
                userId = userId.value,
                latestAccess = now.toString(),
            )

            val jsonData = ObjectMapper.kotlinxSerialization.encodeToString(sessionData)

            val sessionKey = getSessionKey(sessionId)
            commands.set(
                sessionKey,
                jsonData,
                SetArgs().ex(ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60),
            )

            commands.sadd(getUserSessionsKey(userId), sessionId.id)
            commands.expire(getUserSessionsKey(userId), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)

            UserSessionRepository.CreateSessionResult(
                sessionId = sessionId,
                latestAccess = now,
            )
        }
    }

    override fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): UserSessionRepository.VerifySessionResult {
        return redisSpan("verifySession") {
            val now = LocalDateTime.now(ZoneOffset.UTC)

            val sessionKey = getSessionKey(sessionId)
            val jsonData = commands.get(sessionKey) ?: return@redisSpan UserSessionRepository.VerifySessionResult.Failure

            val sessionData = try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("jsonData", jsonData)
                return@redisSpan UserSessionRepository.VerifySessionResult.Failure
            }

            val userId = UserId(sessionData.userId)
            val sessionName = sessionData.sessionName

            commands.expire(sessionKey, ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)
            commands.expire(getUserSessionsKey(userId), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)

            if (sessionName.isNotEmpty()) {
                commands.expire(getUserSessionKey(userId, sessionName), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)
            }

            UserSessionRepository.VerifySessionResult.Success(
                userId = userId,
                sessionId = sessionId,
                latestAccess = now,
            )
        }
    }

    override fun getSessionInfo(sessionId: UserSessionId): UserSessionRepository.SessionInfo? {
        return redisSpan("getSessionInfo") {
            val sessionKey = getSessionKey(sessionId)
            val jsonData = commands.get(sessionKey) ?: return@redisSpan null

            val sessionData = try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("jsonData", jsonData)
                return@redisSpan null
            }

            val latestAccess = try {
                LocalDateTime.parse(sessionData.latestAccess)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("latestAccess", sessionData.latestAccess)
                LocalDateTime.now(ZoneOffset.UTC)
            }

            UserSessionRepository.SessionInfo(
                name = sessionData.sessionName,
                latestAccess = latestAccess,
            )
        }
    }

    override fun getSessions(userId: UserId): List<UserSessionRepository.SessionInfo> {
        return redisSpan("getSessions") {
            val sessionsKey = getUserSessionsKey(userId)
            val sessionIds = commands.smembers(sessionsKey)

            sessionIds.mapNotNull { sessionIdStr ->
                val sessionKey = getSessionKey(UserSessionId(sessionIdStr))
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
                    LocalDateTime.now(ZoneOffset.UTC)
                }

                UserSessionRepository.SessionInfo(
                    name = sessionData.sessionName,
                    latestAccess = latestAccess,
                )
            }
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

        return redisSpan("deleteSession") {
            val userSessionKey = getUserSessionKey(userId, sessionName)
            val sessionIdStr = commands.get(userSessionKey) ?: return@redisSpan false

            val sessionId = UserSessionId(sessionIdStr)
            clearSession(sessionId)

            true
        }
    }

    override fun changeSessionName(
        sessionId: UserSessionId,
        name: String,
    ): UserSessionRepository.SessionInfo? {
        return redisSpan("changeSessionName") {
            val sessionKey = getSessionKey(sessionId)
            val jsonData = commands.get(sessionKey) ?: return@redisSpan null

            val sessionData = try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("jsonData", jsonData)
                return@redisSpan null
            }

            val userId = UserId(sessionData.userId)
            val oldName = sessionData.sessionName
            val latestAccess = try {
                LocalDateTime.parse(sessionData.latestAccess)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("latestAccess", sessionData.latestAccess)
                LocalDateTime.now(ZoneOffset.UTC)
            }

            if (oldName.isNotEmpty()) {
                commands.del(getUserSessionKey(userId, oldName))
            }

            commands.set(getUserSessionKey(userId, name), sessionId.id)

            val updatedSessionData = sessionData.copy(sessionName = name)
            val updatedJsonData = ObjectMapper.kotlinxSerialization.encodeToString(updatedSessionData)
            commands.set(sessionKey, updatedJsonData)

            UserSessionRepository.SessionInfo(
                name = name,
                latestAccess = latestAccess,
            )
        }
    }

    private fun getSessionKey(sessionId: UserSessionId): String = "user_session:${sessionId.id}"

    private fun getUserSessionKey(userId: UserId, sessionName: String): String = "user_session_by_user:${userId.value}:$sessionName"

    private fun getUserSessionsKey(userId: UserId): String = "user_sessions:${userId.value}"

    private inline fun <T> redisSpan(name: String, block: () -> T): T {
        val span = tracer.spanBuilder("redis.$name")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("db.system", "redis")
            .startSpan()
        return try {
            block()
        } catch (e: Throwable) {
            span.setStatus(StatusCode.ERROR)
            span.recordException(e)
            throw e
        } finally {
            span.end()
        }
    }

    @Serializable
    private data class SessionData(
        val userId: Int,
        val sessionName: String = "",
        val latestAccess: String = LocalDateTime.now(ZoneOffset.UTC).toString(),
    )
}
