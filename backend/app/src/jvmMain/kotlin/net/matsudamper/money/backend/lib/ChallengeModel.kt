package net.matsudamper.money.backend.lib

import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import net.matsudamper.money.backend.datasource.challenge.ChallengeRepository

class ChallengeModel(
    private val challengeRepository: ChallengeRepository,
) {
    fun generateChallenge(): String {
        val challenge = UUID.randomUUID().toString()
        challengeRepository.set(challenge, 5.minutes)
        return challenge
    }

    /**
     * @return isSuccess
     */
    fun validateChallenge(challenge: String): Boolean {
        return challengeRepository.containsWithDelete(challenge)
    }
}
