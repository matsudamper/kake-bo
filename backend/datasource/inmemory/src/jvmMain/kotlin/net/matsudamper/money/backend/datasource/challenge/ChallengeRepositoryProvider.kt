package net.matsudamper.money.backend.datasource.challenge

object ChallengeRepositoryProvider {
    fun provideLocalRepository(): ChallengeRepository {
        return LocalChallengeRepository()
    }

    fun provideRedisRepository(host: String, port: Int): ChallengeRepository {
        return RedisChallengeRepository(host, port)
    }
}
