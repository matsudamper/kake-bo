package net.matsudamper.money.backend.di

import net.matsudamper.money.backend.app.interfaces.AdminRepository
import net.matsudamper.money.backend.app.interfaces.AdminSessionRepository
import net.matsudamper.money.backend.app.interfaces.ChallengeRepository
import net.matsudamper.money.backend.app.interfaces.FidoRepository
import net.matsudamper.money.backend.app.interfaces.ImportedMailRepository
import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.app.interfaces.MailRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageAnalyticsRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageSubCategoryRepository
import net.matsudamper.money.backend.app.interfaces.UserConfigRepository
import net.matsudamper.money.backend.app.interfaces.UserLoginRepository
import net.matsudamper.money.backend.app.interfaces.UserRepository
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.base.ServerVariables
import net.matsudamper.money.backend.datasource.challenge.ChallengeRepositoryProvider
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.backend.datasource.db.repository.AdminRepositoryImpl
import net.matsudamper.money.backend.datasource.db.repository.DbAdminSessionRepository
import net.matsudamper.money.backend.datasource.db.repository.DbFidoRepository
import net.matsudamper.money.backend.datasource.db.repository.DbImportedImportedMailRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMailFilterRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsageAnalyticsRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsageCategoryRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsageRepository
import net.matsudamper.money.backend.datasource.db.repository.DbMoneyUsageSubCategoryRepository
import net.matsudamper.money.backend.datasource.db.repository.DbUserConfigRepository
import net.matsudamper.money.backend.datasource.db.repository.DbUserLoginRepository
import net.matsudamper.money.backend.datasource.db.repository.DbUserRepository
import net.matsudamper.money.backend.datasource.db.repository.DbUserSessionRepository
import net.matsudamper.money.backend.mail.MailMailRepository

interface DiContainer {
    fun createMailRepository(
        host: String,
        port: Int,
        userName: String,
        password: String,
    ): MailRepository

    fun createUserSessionRepository(): UserSessionRepository
    fun createUserConfigRepository(): UserConfigRepository
    fun createDbMailRepository(): ImportedMailRepository
    fun createMoneyUsageCategoryRepository(): MoneyUsageCategoryRepository
    fun createMoneyUsageSubCategoryRepository(): MoneyUsageSubCategoryRepository
    fun createMoneyUsageRepository(): MoneyUsageRepository
    fun createMailFilterRepository(): MailFilterRepository
    fun createMoneyUsageAnalyticsRepository(): MoneyUsageAnalyticsRepository
    fun createUserNameRepository(): UserRepository
    fun createFidoRepository(): FidoRepository
    fun createChallengeRepository(): ChallengeRepository
    fun createAdminUserSessionRepository(): AdminSessionRepository
    fun createAdminRepository(): AdminRepository
    fun userLoginRepository(): UserLoginRepository
}

class MainDiContainer : DiContainer {
    override fun createAdminRepository(): AdminRepository {
        return AdminRepositoryImpl()
    }

    override fun createMailRepository(
        host: String,
        port: Int,
        userName: String,
        password: String,
    ): MailRepository {
        return MailMailRepository(
            host = host,
            port = port,
            userName = userName,
            password = password,
        )
    }

    private val dbUserSessionRepository = DbUserSessionRepository()
    override fun createUserSessionRepository(): DbUserSessionRepository {
        return dbUserSessionRepository
    }

    private val mailFilterRepository = DbMailFilterRepository(dbConnection = DbConnectionImpl)
    override fun createMailFilterRepository(): MailFilterRepository {
        return mailFilterRepository
    }

    private val userConfigRepository = DbUserConfigRepository()
    override fun createUserConfigRepository(): DbUserConfigRepository {
        return userConfigRepository
    }

    private val dbImportedMailRepository = DbImportedImportedMailRepository(dbConnection = DbConnectionImpl)
    override fun createDbMailRepository(): DbImportedImportedMailRepository {
        return dbImportedMailRepository
    }

    private val moneyUsageCategoryRepository = DbMoneyUsageCategoryRepository()
    override fun createMoneyUsageCategoryRepository(): MoneyUsageCategoryRepository {
        return moneyUsageCategoryRepository
    }

    private val moneyUsageSubCategoryRepository = DbMoneyUsageSubCategoryRepository()
    override fun createMoneyUsageSubCategoryRepository(): MoneyUsageSubCategoryRepository {
        return moneyUsageSubCategoryRepository
    }

    private val moneyUsageRepository = DbMoneyUsageRepository()
    override fun createMoneyUsageRepository(): MoneyUsageRepository {
        return moneyUsageRepository
    }

    private val dbMoneyUsageAnalyticsRepository = DbMoneyUsageAnalyticsRepository(dbConnection = DbConnectionImpl)
    override fun createMoneyUsageAnalyticsRepository(): DbMoneyUsageAnalyticsRepository {
        return dbMoneyUsageAnalyticsRepository
    }

    private val userRepository = DbUserRepository()
    override fun createUserNameRepository(): UserRepository {
        return userRepository
    }

    private val fidoRepository = DbFidoRepository(dbConnection = DbConnectionImpl)
    override fun createFidoRepository(): FidoRepository {
        return fidoRepository
    }

    private val challengeRepository: ChallengeRepository = if (ServerEnv.enableRedis) {
        ChallengeRepositoryProvider.provideRedisRepository(
            host = ServerEnv.redisHost!!,
            port = ServerEnv.redisPort!!,
            index = ServerVariables.REDIS_INDEX_CHALLENGE,
        )
    } else {
        ChallengeRepositoryProvider.provideLocalRepository()
    }

    override fun createChallengeRepository(): ChallengeRepository {
        return challengeRepository
    }

    override fun createAdminUserSessionRepository(): AdminSessionRepository {
        return DbAdminSessionRepository(dbConnection = DbConnectionImpl)
    }

    override fun userLoginRepository(): UserLoginRepository {
        return DbUserLoginRepository()
    }
}
