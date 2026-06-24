package net.matsudamper.money.backend.datasource.session

import java.time.Clock
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository

object UserSessionRepositoryProvider {
    fun provideRedisRepository(
        host: String,
        port: Int,
        index: Int,
        clock: Clock,
    ): UserSessionRepository {
        return RedisUserSessionRepository(host, port, index, clock)
    }

    fun provideLocalRepository(clock: Clock): UserSessionRepository {
        return LocalUserSessionRepository(clock)
    }
}
