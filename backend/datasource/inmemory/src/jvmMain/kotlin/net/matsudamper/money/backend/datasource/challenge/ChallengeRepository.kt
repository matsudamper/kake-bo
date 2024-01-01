package net.matsudamper.money.backend.datasource.challenge

import kotlin.time.Duration

interface ChallengeRepository {
    fun set(key: String, expire: Duration)
    fun containsWithDelete(key: String): Boolean
}
