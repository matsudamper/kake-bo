package net.matsudamper.money.backend.datasource.session

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.backend.base.ObjectMapper
import net.matsudamper.money.backend.base.ServerVariables
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.element.UserId
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams

internal class RedisUserSessionRepository(
    host: String,
    port: Int,
    private val index: Int,
) : UserSessionRepository {
    private val jedisPool = JedisPool(host, port)

    override fun clearSession(sessionId: UserSessionId) {
        useJedis { jedis ->
            val sessionKey = getSessionKey(sessionId)
            val jsonData = jedis.get(sessionKey)

            if (jsonData != null) {
                val sessionData = try {
                    ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
                } catch (e: Throwable) {
                    TraceLogger.impl().noticeThrowable(e, mapOf("jsonData" to jsonData), true)
                    null
                }

                if (sessionData != null) {
                    val userId = UserId(sessionData.userId)
                    val sessionName = sessionData.sessionName

                    jedis.srem(getUserSessionsKey(userId), sessionId.id)

                    if (sessionName.isNotEmpty()) {
                        jedis.del(getUserSessionKey(userId, sessionName))
                    }
                }
            }

            jedis.del(sessionKey)
        }
    }

    override fun createSession(userId: UserId): UserSessionRepository.CreateSessionResult {
        val sessionId = UserSessionId(UUID.randomUUID().toString().replace("-", ""))
        val now = LocalDateTime.now(ZoneOffset.UTC)

        useJedis { jedis ->
            val sessionData = SessionData(
                userId = userId.value,
                latestAccess = now.toString(),
            )

            val jsonData = ObjectMapper.kotlinxSerialization.encodeToString(sessionData)

            val sessionKey = getSessionKey(sessionId)
            jedis.set(
                sessionKey,
                jsonData,
                SetParams().ex(ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60), // Convert days to seconds
            )

            jedis.sadd(getUserSessionsKey(userId), sessionId.id)
            jedis.expire(getUserSessionsKey(userId), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)
        }

        return UserSessionRepository.CreateSessionResult(
            sessionId = sessionId,
            latestAccess = now,
        )
    }

    override fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): UserSessionRepository.VerifySessionResult {
        val now = LocalDateTime.now(ZoneOffset.UTC)

        return useJedis { jedis ->
            val sessionKey = getSessionKey(sessionId)
            val jsonData = jedis.get(sessionKey) ?: return@useJedis UserSessionRepository.VerifySessionResult.Failure

            val sessionData = try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, mapOf("jsonData" to jsonData), true)
                return@useJedis UserSessionRepository.VerifySessionResult.Failure
            }

            val userId = UserId(sessionData.userId)
            val sessionName = sessionData.sessionName

            jedis.expire(sessionKey, ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)

            jedis.expire(getUserSessionsKey(userId), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)

            if (sessionName.isNotEmpty()) {
                jedis.expire(getUserSessionKey(userId, sessionName), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)
            }

            UserSessionRepository.VerifySessionResult.Success(
                userId = userId,
                sessionId = sessionId,
                latestAccess = now,
            )
        }
    }

    override fun getSessionInfo(sessionId: UserSessionId): UserSessionRepository.SessionInfo? {
        return useJedis { jedis ->
            val sessionKey = getSessionKey(sessionId)
            val jsonData = jedis.get(sessionKey) ?: return@useJedis null

            val sessionData = try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, mapOf("jsonData" to jsonData), true)
                return@useJedis null
            }

            val now = LocalDateTime.now(ZoneOffset.UTC)

            UserSessionRepository.SessionInfo(
                name = sessionData.sessionName,
                latestAccess = now,
            )
        }
    }

    override fun getSessions(userId: UserId): List<UserSessionRepository.SessionInfo> {
        return useJedis { jedis ->
            val sessionsKey = getUserSessionsKey(userId)
            val sessionIds = jedis.smembers(sessionsKey)
            val now = LocalDateTime.now(ZoneOffset.UTC)

            sessionIds.mapNotNull { sessionIdStr ->
                val sessionKey = getSessionKey(UserSessionId(sessionIdStr))
                val jsonData = jedis.get(sessionKey) ?: return@mapNotNull null

                val sessionData = try {
                    ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
                } catch (e: Throwable) {
                    TraceLogger.impl().noticeThrowable(e, mapOf("jsonData" to jsonData), true)
                    return@mapNotNull null
                }

                UserSessionRepository.SessionInfo(
                    name = sessionData.sessionName,
                    latestAccess = now,
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

        return useJedis { jedis ->
            val userSessionKey = getUserSessionKey(userId, sessionName)
            val sessionIdStr = jedis.get(userSessionKey) ?: return@useJedis false

            val sessionId = UserSessionId(sessionIdStr)
            clearSession(sessionId)

            true
        }
    }

    override fun changeSessionName(
        sessionId: UserSessionId,
        name: String,
    ): UserSessionRepository.SessionInfo? {
        return useJedis { jedis ->
            val sessionKey = getSessionKey(sessionId)
            val jsonData = jedis.get(sessionKey) ?: return@useJedis null

            val sessionData = try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, mapOf("jsonData" to jsonData), true)
                return@useJedis null
            }

            val userId = UserId(sessionData.userId)
            val oldName = sessionData.sessionName
            val latestAccess = try {
                LocalDateTime.parse(sessionData.latestAccess)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, mapOf("latestAccess" to sessionData.latestAccess), true)
                LocalDateTime.now(ZoneOffset.UTC)
            }

            if (oldName.isNotEmpty()) {
                jedis.del(getUserSessionKey(userId, oldName))
            }

            jedis.set(getUserSessionKey(userId, name), sessionId.id)

            val updatedSessionData = sessionData.copy(sessionName = name)

            val updatedJsonData = ObjectMapper.kotlinxSerialization.encodeToString(updatedSessionData)

            jedis.set(sessionKey, updatedJsonData)

            UserSessionRepository.SessionInfo(
                name = name,
                latestAccess = latestAccess,
            )
        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun <T> useJedis(block: (jedis: redis.clients.jedis.Jedis) -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        jedisPool.resource.use { jedis ->
            jedis.select(index)
            return block(jedis)
        }
    }

    private fun getSessionKey(sessionId: UserSessionId): String = "user_session:${sessionId.id}"

    private fun getUserSessionKey(userId: UserId, sessionName: String): String = "user_session_by_user:${userId.value}:$sessionName"

    private fun getUserSessionsKey(userId: UserId): String = "user_sessions:${userId.value}"

    @Serializable
    private data class SessionData(
        val userId: Int,
        val sessionName: String = "",
        val latestAccess: String = LocalDateTime.now(ZoneOffset.UTC).toString(),
    )
}
