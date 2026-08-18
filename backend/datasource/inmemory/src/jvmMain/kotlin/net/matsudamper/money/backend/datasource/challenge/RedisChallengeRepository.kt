package net.matsudamper.money.backend.datasource.challenge

import kotlin.time.Duration
import io.lettuce.core.ClientOptions
import io.lettuce.core.MaintNotificationsConfig
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
    private val clientResources: ClientResources = ClientResources.builder()
        .tracing(LettuceTelemetry.create(OpenTelemetryInitializer.get()).createTracing())
        .build()
    private val redisClient: RedisClient = run {
        val uri = RedisURI.Builder.redis(host, port).withDatabase(index).build()
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

    override fun warmup() {
        commands.ping()
    }

    // 外部で生成したClientResourcesはRedisClient.shutdown()では止まらないため個別に終了する
    override fun close() {
        redisClient.shutdown()
        clientResources.shutdown()
    }
}
