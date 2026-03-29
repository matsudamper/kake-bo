package net.matsudamper.money.backend.datasource.challenge

import kotlin.time.Duration
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.resource.ClientResources
import io.opentelemetry.instrumentation.lettuce.v5_1.LettuceTelemetry
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository
import net.matsudamper.money.backend.base.OpenTelemetryInitializer

internal class RedisChallengeRepository(
    host: String,
    port: Int,
    index: Int,
) : ChallengeRepository {
    private val redisClient: RedisClient = run {
        val uri = RedisURI.Builder.redis(host, port).withDatabase(index).build()
        val clientResources = ClientResources.builder()
            .tracing(LettuceTelemetry.create(OpenTelemetryInitializer.get()).createTracing())
            .build()
        RedisClient.create(clientResources, uri)
    }
    private val connection: StatefulRedisConnection<String, String> by lazy { redisClient.connect() }
    private val commands by lazy { connection.sync() }

    override fun set(
        key: String,
        expire: Duration,
    ) {
        commands.set(
            key,
            "",
            SetArgs().px(expire.inWholeMilliseconds),
        )
    }

    override fun containsWithDelete(key: String): Boolean {
        return commands.getdel(key) != null
    }
}
