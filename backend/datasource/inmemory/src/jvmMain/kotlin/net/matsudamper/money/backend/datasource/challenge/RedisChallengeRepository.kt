package net.matsudamper.money.backend.datasource.challenge

import kotlin.time.Duration
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams

internal class RedisChallengeRepository(
    host: String,
    port: Int,
) : ChallengeRepository {
    private val jedisPool = JedisPool(host, port)
    override fun set(key: String, expire: Duration) {
        jedisPool.resource.use { jedis ->
            jedis.set(
                key, "",
                SetParams().also { params ->
                    params.px(expire.inWholeMilliseconds)
                },
            )
        }
    }

    override fun containsWithDelete(key: String): Boolean {
        jedisPool.resource.use { jedis ->
            val result = jedis.exists(key)
            if (result) {
                jedis.del(key)
            }
            return result
        }
    }
}
