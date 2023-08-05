package net.matsudamper.money.backend.di

import net.matsudamper.money.backend.DbConnectionImpl
import net.matsudamper.money.backend.mail.MailRepository
import net.matsudamper.money.backend.repository.DbMailRepository
import net.matsudamper.money.backend.repository.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.repository.MoneyUsageRepository
import net.matsudamper.money.backend.repository.MoneyUsageSubCategoryRepository
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
    fun createMoneyUsageCategoryRepository(): MoneyUsageCategoryRepository
    fun createMoneyUsageSubCategoryRepository(): MoneyUsageSubCategoryRepository
    fun createMoneyUsageRepository(): MoneyUsageRepository
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
}
