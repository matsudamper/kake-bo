package net.matsudamper.money.backend.di

import net.matsudamper.money.backend.mail.MailRepository
import net.matsudamper.money.backend.repository.DbMailRepository
import net.matsudamper.money.backend.repository.UserConfigRepository

interface RepositoryFactory {
    fun createMailRepository(
        host: String,
        port: Int,
        userName: String,
        password: String,
    ): MailRepository

    fun createUserConfigRepository(): UserConfigRepository
    fun createDbMailRepository(): DbMailRepository
}

class RepositoryFactoryImpl : RepositoryFactory {
    override fun createMailRepository(
        host: String,
        port: Int,
        userName: String,
        password: String,
    ): MailRepository {
        return MailRepository(
            host = host,
            port = port,
            userName = userName,
            password = password,
        )
    }

    private val userConfigRepository = UserConfigRepository()
    override fun createUserConfigRepository(): UserConfigRepository {
        return userConfigRepository
    }

    private val dbMailRepository = DbMailRepository()
    override fun createDbMailRepository(): DbMailRepository {
        return dbMailRepository
    }
}
