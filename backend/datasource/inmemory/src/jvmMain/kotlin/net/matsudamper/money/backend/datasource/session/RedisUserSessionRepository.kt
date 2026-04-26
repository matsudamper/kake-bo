package net.matsudamper.money.backend.datasource.session

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import io.lettuce.core.ClientOptions
import io.lettuce.core.MaintNotificationsConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.resource.ClientResources
import io.opentelemetry.instrumentation.lettuce.v5_1.LettuceTelemetry
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.UserSessionId
import net.matsudamper.money.backend.base.ObjectMapper
import net.matsudamper.money.backend.base.OpenTelemetryInitializer
import net.matsudamper.money.backend.base.ServerVariables
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.element.SessionRecordId
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
        val sessionKey = SessionKeys.Session(sessionId)
        val sessionData = sessionKey.get(commands) ?: return

        SessionKeys.UserSessionByUser(sessionData.userId)
            .delete(commands, sessionRecordId = sessionData.sessionRecordId)

        SessionKeys.UserSessionRecord(recordId = sessionData.sessionRecordId)
            .delete(commands)
        sessionKey.delete(commands)
    }

    override fun createSession(userId: UserId): UserSessionRepository.CreateSessionResult {
        val sessionId = UserSessionId(UUID.randomUUID().toString().replace("-", ""))
        val now = LocalDateTime.now(clock)

        val sessionRecordId = SessionRecordId(UUID.randomUUID().toString())

        SessionKeys.Session(sessionId)
            .set(
                commands,
                SessionKeys.Session.SessionData(
                    userId = userId,
                    sessionRecordId = sessionRecordId,
                    createdAt = Instant.now(clock).toKotlinInstant(),
                    lastAccess = Instant.now(clock).toKotlinInstant(),
                    name = UUID.randomUUID().toString().replace("-", ""),
                ),
            )

        SessionKeys.UserSessionRecord(sessionRecordId).set(
            commands,
            SessionKeys.UserSessionRecord.Data(
                userSessionId = sessionId,
            ),
        )
        SessionKeys.UserSessionByUser(userId)
            .add(
                commands,
                sessionRecordId,
            )

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

        val sessionKey = SessionKeys.Session(sessionId)
        val sessionData = sessionKey.get(commands) ?: return UserSessionRepository.VerifySessionResult.Failure

        val sessionRecordId = sessionData.sessionRecordId

        SessionKeys.UserSessionRecord(sessionRecordId)
            .updateExpire(commands)

        SessionKeys.UserSessionByUser(sessionData.userId)
            .updateExpire(commands)

        return UserSessionRepository.VerifySessionResult.Success(
            userId = sessionData.userId,
            sessionId = sessionId,
            latestAccess = now,
        )
    }

    override fun getSessionInfo(sessionId: UserSessionId): UserSessionRepository.SessionInfo? {
        val sessionData = SessionKeys.Session(sessionId).get(commands) ?: return null

        return UserSessionRepository.SessionInfo(
            sessionRecordId = sessionData.sessionRecordId,
            name = sessionData.name,
            latestAccess = sessionData.lastAccess.toJavaInstant(),
        )
    }

    override fun getSessions(userId: UserId): List<UserSessionRepository.SessionInfo> {
        val sessionsKey = SessionKeys.UserSessionByUser(userId)
        val recordIdList = sessionsKey.get(commands)
        return recordIdList.mapNotNull { recordId ->
            val sessionId = SessionKeys.UserSessionRecord(recordId).get(commands)?.userSessionId ?: return@mapNotNull null
            val sessionData = SessionKeys.Session(sessionId).get(commands) ?: return@mapNotNull null
            UserSessionRepository.SessionInfo(
                sessionRecordId = recordId,
                name = sessionData.name,
                latestAccess = sessionData.lastAccess.toJavaInstant(),
            )
        }
    }

    override fun deleteSession(
        currentSessionId: UserSessionId,
        targetSessionRecordId: SessionRecordId,
    ): Boolean {
        val userSessionRecord = SessionKeys.UserSessionRecord(targetSessionRecordId)
        val recordData = userSessionRecord.get(commands) ?: return false
        val currentSessionUserId = SessionKeys.Session(currentSessionId).get(commands)?.userId ?: return false
        val targetSessionUserId = SessionKeys.Session(recordData.userSessionId).get(commands)?.userId ?: return false
        if (currentSessionUserId != targetSessionUserId) return false
        if (recordData.userSessionId == currentSessionId) return false

        clearSession(recordData.userSessionId)
        return true
    }

    override fun changeSessionName(
        currentSessionId: UserSessionId,
        sessionRecordId: SessionRecordId,
        sessionName: String,
    ): UserSessionRepository.SessionInfo? {
        val currentSessionUserId = SessionKeys.Session(currentSessionId).get(commands)?.userId ?: return null
        val recordKey = SessionKeys.UserSessionRecord(sessionRecordId)
        val recordData = recordKey.get(commands) ?: return null

        val sessionKey = SessionKeys.Session(recordData.userSessionId)
        val sessionData = sessionKey.get(commands) ?: return null

        if (sessionData.userId != currentSessionUserId) return null

        sessionKey.update(
            commands,
            sessionData.copy(
                name = sessionName,
            ),
        )

        return UserSessionRepository.SessionInfo(
            sessionRecordId = sessionRecordId,
            name = sessionName,
            latestAccess = sessionData.lastAccess.toJavaInstant(),
        )
    }
}

private sealed interface SessionKeys {
    val key: String

    fun delete(commands: RedisCommands<String, String>) {
        commands.del(key)
    }

    /**
     * セッション管理のメイン
     */
    class Session(sessionId: UserSessionId) : SessionKeys {
        override val key: String = "user_session:${sessionId.id}"

        fun set(
            commands: RedisCommands<String, String>,
            data: SessionData,
        ): String? {
            return commands.set(
                key,
                ObjectMapper.kotlinxSerialization.encodeToString(data),
                SetArgs()
                    .nx()
                    .ex(ServerVariables.USER_SESSION_EXPIRE_DAY.days.inWholeSeconds),
            )
        }

        fun update(commands: RedisCommands<String, String>, data: SessionData): String? {
            return commands.set(
                key,
                ObjectMapper.kotlinxSerialization.encodeToString(data),
                SetArgs().keepttl(),
            )
        }

        fun get(
            commands: RedisCommands<String, String>,
        ): SessionData? {
            val jsonData = commands.get(key) ?: return null
            return try {
                ObjectMapper.kotlinxSerialization.decodeFromString<SessionData>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("jsonData", jsonData)
                null
            }
        }

        @Serializable
        data class SessionData(
            @Serializable(UserIdSerializer::class)
            val userId: UserId,
            @Serializable(SessionRecordIdSerializer::class)
            val sessionRecordId: SessionRecordId,
            val createdAt: kotlin.time.Instant,
            val lastAccess: kotlin.time.Instant,
            val name: String,
        )
    }

    /**
     * [UserId]ごとの[SessionRecordId]のセット
     */
    class UserSessionByUser(userId: UserId) : SessionKeys {
        override val key: String = "user_session_by_user:${userId.value}"

        private val expireSecond = ServerVariables.USER_SESSION_EXPIRE_DAY.days.inWholeSeconds
            .plus(10.seconds.inWholeSeconds) // 本体のセッションより少し長めに設定

        fun add(
            commands: RedisCommands<String, String>,
            sessionRecordId: SessionRecordId,
        ) {
            commands.sadd(
                key,
                sessionRecordId.value,
            )
            commands.expire(key, expireSecond)
        }

        fun get(commands: RedisCommands<String, String>): List<SessionRecordId> {
            val jsonDataList = commands.smembers(key)
            return jsonDataList.mapNotNull { str ->
                SessionRecordId(str)
            }
        }

        fun delete(
            commands: RedisCommands<String, String>,
            sessionRecordId: SessionRecordId,
        ) {
            commands.srem(
                key,
                sessionRecordId.value,
            )
        }

        fun updateExpire(
            commands: RedisCommands<String, String>,
        ) {
            commands.expire(key, expireSecond)
        }
    }

    /**
     * [SessionRecordId]を[UserSessionId]に紐づける。
     */
    class UserSessionRecord(recordId: SessionRecordId) : SessionKeys {
        override val key: String = "user_session_record:${recordId.value}"
        private val expireSecond = ServerVariables.USER_SESSION_EXPIRE_DAY.days.inWholeSeconds
            .plus(10.seconds.inWholeSeconds) // 本体のセッションより少し長めに設定

        fun set(
            commands: RedisCommands<String, String>,
            data: Data,
        ): String? {
            return commands.set(
                key,
                ObjectMapper.kotlinxSerialization.encodeToString(data),
                SetArgs()
                    .nx()
                    .ex(expireSecond),
            )
        }

        fun get(commands: RedisCommands<String, String>): Data? {
            val jsonData = commands.get(key) ?: return null

            return try {
                ObjectMapper.kotlinxSerialization.decodeFromString<Data>(jsonData)
            } catch (e: Throwable) {
                TraceLogger.impl().noticeThrowable(e, true)
                TraceLogger.impl().setAttribute("jsonData", jsonData)
                null
            }
        }

        fun updateExpire(
            commands: RedisCommands<String, String>,
        ) {
            commands.expire(key, expireSecond)
        }

        @Serializable
        data class Data(
            @Serializable(UserSessionIdSerializer::class)
            val userSessionId: UserSessionId,
        )
    }
}

private class UserSessionIdSerializer : KSerializer<UserSessionId> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UserSessionId")
    override fun deserialize(decoder: Decoder): UserSessionId {
        return UserSessionId(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UserSessionId) {
        encoder.encodeString(value.id)
    }
}

private class SessionRecordIdSerializer : KSerializer<SessionRecordId> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SessionRecordId")
    override fun deserialize(decoder: Decoder): SessionRecordId {
        return SessionRecordId(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: SessionRecordId) {
        encoder.encodeString(value.value)
    }
}

private class UserIdSerializer : KSerializer<UserId> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UserId")
    override fun deserialize(decoder: Decoder): UserId {
        return UserId(decoder.decodeInt())
    }

    override fun serialize(encoder: Encoder, value: UserId) {
        encoder.encodeInt(value.value)
    }
}
