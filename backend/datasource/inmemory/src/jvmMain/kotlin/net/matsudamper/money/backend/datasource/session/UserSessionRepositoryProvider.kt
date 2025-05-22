package net.matsudamper.money.backend.datasource.session

import net.matsudamper.money.backend.app.interfaces.UserSessionRepository

object UserSessionRepositoryProvider {
    fun provideRedisRepository(
        host: String,
        port: Int,
        index: Int,
    ): UserSessionRepository {
        return RedisUserSessionRepository(host, port, index)
    }

    fun provideLocalRepository(): UserSessionRepository {
        return LocalUserSessionRepository()
    }
}
