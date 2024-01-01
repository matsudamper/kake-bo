package net.matsudamper.money.backend.di

import net.matsudamper.money.backend.DbConnectionImpl
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.datasource.challenge.ChallengeRepository
import net.matsudamper.money.backend.datasource.challenge.ChallengeRepositoryProvider
import net.matsudamper.money.backend.mail.MailRepository
import net.matsudamper.money.backend.repository.DbMailRepository
import net.matsudamper.money.backend.repository.FidoRepository
import net.matsudamper.money.backend.repository.MailFilterRepository
import net.matsudamper.money.backend.repository.MoneyUsageAnalyticsRepository
import net.matsudamper.money.backend.repository.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.repository.MoneyUsageRepository
import net.matsudamper.money.backend.repository.MoneyUsageSubCategoryRepository
import net.matsudamper.money.backend.repository.UserConfigRepository
import net.matsudamper.money.backend.repository.UserRepository

interface RepositoryFactory {
    fun createMailRepository(
        host: String,
        port: Int,
        userName: String,
        password: String,
    ): MailRepository

    fun createUserConfigRepository(): UserConfigRepository
    fun createDbMailRepository(): DbMailRepository
    fun createMoneyUsageCategoryRepository(): MoneyUsageCategoryRepository
    fun createMoneyUsageSubCategoryRepository(): MoneyUsageSubCategoryRepository
    fun createMoneyUsageRepository(): MoneyUsageRepository
    fun createMailFilterRepository(): MailFilterRepository
    fun createMoneyUsageAnalyticsRepository(): MoneyUsageAnalyticsRepository
    fun createUserNameRepository(): UserRepository
    fun createFidoRepository(): FidoRepository
    fun createChallengeRepository(): ChallengeRepository
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

    private val mailFilterRepository = MailFilterRepository(dbConnection = DbConnectionImpl)
    override fun createMailFilterRepository(): MailFilterRepository {
        return mailFilterRepository
    }

    private val userConfigRepository = UserConfigRepository()
    override fun createUserConfigRepository(): UserConfigRepository {
        return userConfigRepository
    }

    private val dbMailRepository = DbMailRepository(dbConnection = DbConnectionImpl)
    override fun createDbMailRepository(): DbMailRepository {
        return dbMailRepository
    }

    private val moneyUsageCategoryRepository = MoneyUsageCategoryRepository()
    override fun createMoneyUsageCategoryRepository(): MoneyUsageCategoryRepository {
        return moneyUsageCategoryRepository
    }

    private val moneyUsageSubCategoryRepository = MoneyUsageSubCategoryRepository()
    override fun createMoneyUsageSubCategoryRepository(): MoneyUsageSubCategoryRepository {
        return moneyUsageSubCategoryRepository
    }

    private val moneyUsageRepository = MoneyUsageRepository()
    override fun createMoneyUsageRepository(): MoneyUsageRepository {
        return moneyUsageRepository
    }

    private val moneyUsageAnalyticsRepository = MoneyUsageAnalyticsRepository(dbConnection = DbConnectionImpl)
    override fun createMoneyUsageAnalyticsRepository(): MoneyUsageAnalyticsRepository {
        return moneyUsageAnalyticsRepository
    }

    private val userRepository = UserRepository()
    override fun createUserNameRepository(): UserRepository {
        return userRepository
    }

    private val fidoRepository = FidoRepository(dbConnection = DbConnectionImpl)
    override fun createFidoRepository(): FidoRepository {
        return fidoRepository
    }

    private val challengeRepository: ChallengeRepository = if (ServerEnv.enableRedis) {
        ChallengeRepositoryProvider.provideRedisRepository(
            host = ServerEnv.redisHost,
            port = ServerEnv.redisPort,
        )
    } else {
        ChallengeRepositoryProvider.provideLocalRepository()
    }

    override fun createChallengeRepository(): ChallengeRepository {
        return challengeRepository
    }
}
