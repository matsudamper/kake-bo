package net.matsudamper.money.backend.datasource.challenge

import kotlin.time.Duration
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository
import net.matsudamper.money.backend.base.OpenTelemetryInitializer

internal class RedisChallengeRepository(
    host: String,
    port: Int,
    index: Int,
) : ChallengeRepository {
    private val tracer = OpenTelemetryInitializer.get().getTracer("redis.lettuce")
    private val redisClient: RedisClient = run {
        val uri = RedisURI.Builder.redis(host, port).withDatabase(index).build()
        RedisClient.create(uri)
    }
    private val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val commands = connection.sync()

    override fun set(
        key: String,
        expire: Duration,
    ) {
        redisSpan("redis.set") {
            commands.set(
                key,
                "",
                SetArgs().px(expire.inWholeMilliseconds),
            )
        }
    }

    override fun containsWithDelete(key: String): Boolean {
        return redisSpan("redis.getdel") {
            commands.getdel(key) != null
        }
    }

    private inline fun <T> redisSpan(name: String, block: () -> T): T {
        val span = tracer.spanBuilder(name)
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
}
