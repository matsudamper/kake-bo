package net.matsudamper.money.backend.datasource.session

import java.time.Clock
import net.matsudamper.money.backend.app.interfaces.AdminSessionRepository

object AdminSessionRepositoryProvider {
    fun provideRedisRepository(
        host: String,
        port: Int,
        index: Int,
        clock: Clock,
    ): AdminSessionRepository {
        return RedisAdminSessionRepository(host, port, index, clock)
    }
}
