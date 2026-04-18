package net.matsudamper.money.backend.datasource.challenge

import java.time.Clock
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository

object ChallengeRepositoryProvider {
    fun provideLocalRepository(clock: Clock): ChallengeRepository {
        return LocalChallengeRepository(clock)
    }

    fun provideRedisRepository(
        host: String,
        port: Int,
        index: Int,
    ): ChallengeRepository {
        return RedisChallengeRepository(host, port, index)
    }
}
