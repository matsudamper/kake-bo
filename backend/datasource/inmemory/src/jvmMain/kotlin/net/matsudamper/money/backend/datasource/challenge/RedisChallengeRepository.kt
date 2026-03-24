package net.matsudamper.money.backend.datasource.challenge

import kotlin.time.Duration
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.resource.ClientResources
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository
import net.matsudamper.money.backend.base.OpenTelemetryInitializer
import net.matsudamper.money.backend.datasource.redis.LettuceOtelTracing

internal class RedisChallengeRepository(
    host: String,
    port: Int,
    index: Int,
) : ChallengeRepository {
    private val redisClient: RedisClient = run {
        val uri = RedisURI.Builder.redis(host, port).withDatabase(index).build()
        val clientResources = ClientResources.builder()
            .tracing(LettuceOtelTracing(OpenTelemetryInitializer.get()))
            .build()
        RedisClient.create(clientResources, uri)
    }
    private val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val commands = connection.sync()

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
        val exists = commands.exists(key) > 0
        if (exists) {
            commands.del(key)
        }
        return exists
    }
}
