package net.matsudamper.money.backend.datasource.session

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.backend.base.ServerVariables
import net.matsudamper.money.element.UserId
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams

internal class RedisUserSessionRepository(
    host: String,
    port: Int,
    private val index: Int,
) : UserSessionRepository {
    private val jedisPool = JedisPool(host, port)

    // Key format: user_session:{sessionId}
    private fun getSessionKey(sessionId: UserSessionId): String = "user_session:${sessionId.id}"

    // Key format: user_session_by_user:{userId}:{sessionName}
    private fun getUserSessionKey(userId: UserId, sessionName: String): String = "user_session_by_user:${userId.value}:$sessionName"

    // Key format: user_sessions:{userId}
    private fun getUserSessionsKey(userId: UserId): String = "user_sessions:${userId.value}"

    override fun clearSession(sessionId: UserSessionId) {
        useJedis { jedis ->
            // Get user ID and session name before deleting
            val sessionKey = getSessionKey(sessionId)
            val sessionData = jedis.get(sessionKey)

            if (sessionData != null) {
                val parts = sessionData.split("|")
                if (parts.size >= 2) {
                    val userId = UserId(parts[0].toInt())
                    val sessionName = parts[1]

                    // Remove from user's sessions list
                    jedis.srem(getUserSessionsKey(userId), sessionId.id)

                    // Remove user session mapping
                    if (sessionName.isNotEmpty()) {
                        jedis.del(getUserSessionKey(userId, sessionName))
                    }
                }
            }

            // Delete the session
            jedis.del(sessionKey)
        }
    }

    override fun createSession(userId: UserId): UserSessionRepository.CreateSessionResult {
        val sessionId = UserSessionId(UUID.randomUUID().toString().replace("-", ""))
        val now = LocalDateTime.now(ZoneOffset.UTC)

        useJedis { jedis ->
            // Store session data: userId|sessionName
            val sessionKey = getSessionKey(sessionId)
            jedis.set(
                sessionKey,
                "${userId.value}||",
                SetParams().ex(ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60) // Convert days to seconds
            )

            // Add to user's sessions list
            jedis.sadd(getUserSessionsKey(userId), sessionId.id)
            // Set expiration on the user's sessions list too
            jedis.expire(getUserSessionsKey(userId), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)
        }

        return UserSessionRepository.CreateSessionResult(
            sessionId = sessionId,
            latestAccess = now, // Still return current time for API compatibility
        )
    }

    override fun verifySession(
        sessionId: UserSessionId,
        expireDay: Long,
    ): UserSessionRepository.VerifySessionResult {
        val now = LocalDateTime.now(ZoneOffset.UTC)

        return useJedis { jedis ->
            val sessionKey = getSessionKey(sessionId)
            val sessionData = jedis.get(sessionKey) ?: return@useJedis UserSessionRepository.VerifySessionResult.Failure

            val parts = sessionData.split("|")
            if (parts.size < 2) return@useJedis UserSessionRepository.VerifySessionResult.Failure

            val userId = UserId(parts[0].toInt())
            val sessionName = parts[1]

            // Refresh expiration time
            jedis.expire(sessionKey, ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)

            // Also refresh user sessions list expiration
            jedis.expire(getUserSessionsKey(userId), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)

            // If there's a session name, refresh its expiration too
            if (sessionName.isNotEmpty()) {
                jedis.expire(getUserSessionKey(userId, sessionName), ServerVariables.USER_SESSION_EXPIRE_DAY * 24 * 60 * 60)
            }

            UserSessionRepository.VerifySessionResult.Success(
                userId = userId,
                sessionId = sessionId,
                latestAccess = now, // Still return current time for API compatibility
            )
        }
    }

    override fun getSessionInfo(sessionId: UserSessionId): UserSessionRepository.SessionInfo? {
        return useJedis { jedis ->
            val sessionKey = getSessionKey(sessionId)
            val sessionData = jedis.get(sessionKey) ?: return@useJedis null

            val parts = sessionData.split("|")
            if (parts.size < 2) return@useJedis null

            val sessionName = parts[1]
            // We no longer store latest access time, so use current time
            val now = LocalDateTime.now(ZoneOffset.UTC)

            UserSessionRepository.SessionInfo(
                name = sessionName,
                latestAccess = now, // Use current time for API compatibility
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
                val sessionData = jedis.get(sessionKey) ?: return@mapNotNull null

                val parts = sessionData.split("|")
                if (parts.size < 2) return@mapNotNull null

                val sessionName = parts[1]

                UserSessionRepository.SessionInfo(
                    name = sessionName,
                    latestAccess = now, // Use current time for API compatibility
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
            val sessionData = jedis.get(sessionKey) ?: return@useJedis null

            val parts = sessionData.split("|")
            if (parts.size < 3) return@useJedis null

            val userId = UserId(parts[0].toInt())
            val oldName = parts[1]
            val latestAccess = LocalDateTime.parse(parts[2])

            // Remove old name mapping if it exists
            if (oldName.isNotEmpty()) {
                jedis.del(getUserSessionKey(userId, oldName))
            }

            // Add new name mapping
            jedis.set(getUserSessionKey(userId, name), sessionId.id)

            // Update session data
            jedis.set(sessionKey, "${userId.value}|$name|$latestAccess")

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
}
