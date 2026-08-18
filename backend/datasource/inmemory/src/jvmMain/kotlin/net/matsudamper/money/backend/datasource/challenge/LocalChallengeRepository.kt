package net.matsudamper.money.backend.datasource.challenge

import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository

internal class LocalChallengeRepository(
    private val clock: Clock,
) : ChallengeRepository {
    // DiContainerがSingletonになり複数リクエストから同時に触られる
    private val repository = ConcurrentHashMap<String, Data>()

    override fun set(
        key: String,
        expire: Duration,
    ) {
        deleteAfterExpire()
        repository[key] = Data(LocalDateTime.now(clock).plusSeconds(expire.inWholeSeconds))
    }

    override fun containsWithDelete(key: String): Boolean {
        deleteAfterExpire()
        // 同じチャレンジの並行検証で両方が成功しないようにアトミックに取得と削除を行う
        return repository.remove(key) != null
    }

    // プロセス内のみで完結するため事前に確立する接続も解放する資源も持たない
    override fun warmup() = Unit

    override fun close() = Unit

    private fun deleteAfterExpire() {
        val now = LocalDateTime.now(clock)
        // キーだけで削除すると、走査後にset()が書いた新しいチャレンジまで消えてしまう
        repository
            .filter { (_, value) -> value.expire.isBefore(now) }
            .forEach { (key, value) -> repository.remove(key, value) }
    }

    private data class Data(
        val expire: LocalDateTime,
    )
}
