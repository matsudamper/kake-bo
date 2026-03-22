package net.matsudamper.money.backend.datasource.challenge

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository
import redis.clients.jedis.ClientSetInfoConfig
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.params.SetParams

internal class RedisChallengeRepository(
    host: String,
    port: Int,
    private val index: Int,
) : ChallengeRepository {
    // Jedis 7.xではデフォルトで接続時にCLIENT SETINFOコマンドを送信する。
    // その際、DriverInfo.BuilderがJedisMetaInfo.getArtifactId()でpom.propertiesからartifactIdを読み込むが、
    // GraalVM native imageではこのリソース読み込みが失敗しnullが返るため、
    // SafeEncoder.encode()で"null value cannot be sent to redis"エラーが発生する。
    // ClientSetInfoConfig.DISABLEDを設定してCLIENT SETINFOを無効化することで回避する。
    private val jedisPool = JedisPool(
        HostAndPort(host, port),
        DefaultJedisClientConfig.builder()
            .clientSetInfoConfig(ClientSetInfoConfig.DISABLED)
            .build(),
    )

    override fun set(
        key: String,
        expire: Duration,
    ) {
        useJedis { jedis ->
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
        useJedis { jedis ->
            val result = jedis.exists(key)
            if (result) {
                jedis.del(key)
            }
            return result
        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun useJedis(block: (jedis: redis.clients.jedis.Jedis) -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        jedisPool.resource.use { jedis ->
            jedis.select(index)
            block(jedis)
        }
    }
}
