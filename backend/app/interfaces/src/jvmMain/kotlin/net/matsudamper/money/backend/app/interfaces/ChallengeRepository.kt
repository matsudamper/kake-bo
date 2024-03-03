package net.matsudamper.money.backend.app.interfaces

import kotlin.time.Duration

interface ChallengeRepository {
    fun set(key: String, expire: Duration)
    fun containsWithDelete(key: String): Boolean
}
