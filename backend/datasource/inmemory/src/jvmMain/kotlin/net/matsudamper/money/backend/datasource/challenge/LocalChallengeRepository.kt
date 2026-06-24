package net.matsudamper.money.backend.datasource.challenge

import java.time.Clock
import java.time.LocalDateTime
import kotlin.time.Duration
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository

internal class LocalChallengeRepository(
    private val clock: Clock,
) : ChallengeRepository {
    private val repository: MutableMap<String, Data> = mutableMapOf()

    override fun set(
        key: String,
        expire: Duration,
    ) {
        deleteAfterExpire()
        repository[key] = Data(LocalDateTime.now(clock).plusSeconds(expire.inWholeSeconds))
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
            .filter { (_, value) -> value.expire.isBefore(LocalDateTime.now(clock)) }
            .forEach { (key, _) -> repository.remove(key) }
    }

    private data class Data(
        val expire: LocalDateTime,
    )
}
