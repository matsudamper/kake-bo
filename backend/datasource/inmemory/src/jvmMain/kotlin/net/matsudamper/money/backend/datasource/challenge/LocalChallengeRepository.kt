package net.matsudamper.money.backend.datasource.challenge

import java.time.LocalDateTime
import kotlin.time.Duration

internal class LocalChallengeRepository : ChallengeRepository {
    private val repository: MutableMap<String, Data> = mutableMapOf()
    override fun set(key: String, expire: Duration) {
        deleteAfterExpire()
        repository[key] = Data(LocalDateTime.now().plusSeconds(expire.inWholeSeconds))
    }

    override fun containsWithDelete(key: String): Boolean {
        deleteAfterExpire()
        return if (repository.contains(key)) {
            repository.remove(key)
            true
        } else {
            false
        }
    }

    private fun deleteAfterExpire() {
        repository
            .filter { (_, value) -> value.expire.isBefore(LocalDateTime.now()) }
            .forEach { (key, _) -> repository.remove(key) }
    }

    private data class Data(
        val expire: LocalDateTime,
    )
}
