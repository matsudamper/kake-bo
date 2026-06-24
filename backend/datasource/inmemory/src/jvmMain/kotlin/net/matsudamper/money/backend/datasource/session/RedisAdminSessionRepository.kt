package net.matsudamper.money.backend.datasource.session

import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import io.lettuce.core.ClientOptions
import io.lettuce.core.MaintNotificationsConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.resource.ClientResources
import io.opentelemetry.instrumentation.lettuce.v5_1.LettuceTelemetry
import net.matsudamper.money.backend.app.interfaces.AdminSessionRepository
import net.matsudamper.money.backend.app.interfaces.element.AdminSession
import net.matsudamper.money.backend.app.interfaces.element.AdminSessionId
import net.matsudamper.money.backend.base.OpenTelemetryInitializer
import net.matsudamper.money.backend.base.ServerVariables

internal class RedisAdminSessionRepository(
    host: String,
    port: Int,
    index: Int,
    private val clock: Clock,
) : AdminSessionRepository {
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

    override fun verifySession(adminSessionId: String): AdminSession? {
        val key = sessionKey(adminSessionId)
        commands.get(key) ?: return null
        commands.expire(key, ServerVariables.ADMIN_SESSION_EXPIRE_SECONDS)
        return AdminSession(
            adminSessionId = AdminSessionId(adminSessionId),
            expire = LocalDateTime.now(clock).plusSeconds(ServerVariables.ADMIN_SESSION_EXPIRE_SECONDS),
        )
    }

    override fun createSession(): AdminSession {
        val sessionId = UUID.randomUUID().toString().replace("-", "")
        val key = sessionKey(sessionId)
        commands.set(key, sessionId, SetArgs().ex(ServerVariables.ADMIN_SESSION_EXPIRE_SECONDS))
        return AdminSession(
            adminSessionId = AdminSessionId(sessionId),
            expire = LocalDateTime.now(clock).plusSeconds(ServerVariables.ADMIN_SESSION_EXPIRE_SECONDS),
        )
    }

    private fun sessionKey(sessionId: String): String = "admin_session:$sessionId"
}
