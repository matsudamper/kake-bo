package net.matsudamper.money.backend.datasource.challenge

import kotlin.time.Duration
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.RedisClient
import redis.clients.jedis.params.SetParams

internal class RedisChallengeRepository(
    host: String,
    port: Int,
    index: Int,
) : ChallengeRepository {
    private val redisClient = RedisClient.builder()
        .hostAndPort(host, port)
        .clientConfig(
            DefaultJedisClientConfig.builder()
                .database(index)
                .build()
        )
        .build()


    override fun set(
        key: String,
        expire: Duration,
    ) {
        redisClient.use { jedis ->
            jedis.set(
                key,
                "",
                SetParams().also { params ->
                    params.px(expire.inWholeMilliseconds)
                },
            )
        }
    }

    override fun containsWithDelete(key: String): Boolean {
        redisClient.use { jedis ->
            val result = jedis.exists(key)
            if (result) {
                jedis.del(key)
            }
            return result
        }
    }
}
