package net.matsudamper.money.backend.datasource.db.repository

import java.time.LocalDateTime
import net.matsudamper.money.backend.app.interfaces.MoneyUsageAnalyticsRepository
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.db.schema.tables.JMoneyUsageCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class DbMoneyUsageAnalyticsRepository(
    private val dbConnection: DbConnection,
) : MoneyUsageAnalyticsRepository {
    private val usage = JMoneyUsages.MONEY_USAGES
    private val categories = JMoneyUsageCategories.MONEY_USAGE_CATEGORIES
    private val subCategories = JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES

    override fun getTotalAmount(
        userId: UserId,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Long? {
        return runCatching {
            dbConnection.use { connection ->
                val count = DSL.coalesce(DSL.sum(usage.AMOUNT), 0.toBigDecimal())
                val result =
                    DSL.using(connection)
                        .select(count)
                        .from(usage)
                        .where(usage.USER_ID.eq(userId.value))
                        .and(
                            usage.DATETIME.greaterOrEqual(sinceDateTimeAt)
                                .and(usage.DATETIME.lessThan(untilDateTimeAt)),
                        )
                        .fetchOne()

                result?.get(count)!!.toLong()
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { null },
        )
    }

    override fun getTotalAmountBySubCategories(
        userId: UserId,
        categoryIds: List<MoneyUsageCategoryId>,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Result<List<MoneyUsageAnalyticsRepository.TotalAmountBySubCategory>> {
        return runCatching {
            dbConnection.use { connection ->
                val amount = DSL.coalesce(DSL.sum(usage.AMOUNT), 0.toBigDecimal())
                val results =
                    DSL.using(connection)
                        .select(amount, subCategories.MONEY_USAGE_SUB_CATEGORY_ID, subCategories.MONEY_USAGE_CATEGORY_ID)
                        .from(usage)
                        .join(subCategories).using(usage.MONEY_USAGE_SUB_CATEGORY_ID)
                        .join(categories).using(subCategories.MONEY_USAGE_CATEGORY_ID)
                        .where(
                            DSL.value(true)
                                .and(usage.USER_ID.eq(userId.value))
                                .and(usage.DATETIME.greaterOrEqual(sinceDateTimeAt))
                                .and(usage.DATETIME.lessThan(untilDateTimeAt))
                                .and(subCategories.MONEY_USAGE_CATEGORY_ID.`in`(categoryIds.map { it.value })),
                        )
                        .groupBy(subCategories.MONEY_USAGE_SUB_CATEGORY_ID)
                        .fetch()

                val groupedResult =
                    results.groupBy {
                        MoneyUsageCategoryId(it.get(categories.MONEY_USAGE_CATEGORY_ID)!!)
                    }
                categoryIds.map { categoryId ->
                    val result = groupedResult[categoryId]

                    MoneyUsageAnalyticsRepository.TotalAmountBySubCategory(
                        categoryId = categoryId,
                        subCategories =
                        result.orEmpty().map {
                            MoneyUsageAnalyticsRepository.SubCategoryTotalAmount(
                                id = MoneyUsageSubCategoryId(it.get(subCategories.MONEY_USAGE_SUB_CATEGORY_ID)!!),
                                totalAmount = it.get(amount).toLong(),
                            )
                        },
                    )
                }
            }
        }
    }

    override fun getTotalAmountByCategories(
        userId: UserId,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Result<MutableList<MoneyUsageAnalyticsRepository.TotalAmountByCategory>> {
        return runCatching {
            dbConnection.use { connection ->
                val amount = DSL.sum(usage.AMOUNT)
                val result =
                    DSL.using(connection)
                        .select(amount, categories.MONEY_USAGE_CATEGORY_ID)
                        .from(usage)
                        .join(subCategories).using(usage.MONEY_USAGE_SUB_CATEGORY_ID)
                        .join(categories).using(subCategories.MONEY_USAGE_CATEGORY_ID)
                        .where(
                            DSL.value(true)
                                .and(usage.USER_ID.eq(userId.value))
                                .and(usage.DATETIME.greaterOrEqual(sinceDateTimeAt))
                                .and(usage.DATETIME.lessThan(untilDateTimeAt)),
                        )
                        .groupBy(categories.MONEY_USAGE_CATEGORY_ID)
                        .fetch()

                result.map {
                    MoneyUsageAnalyticsRepository.TotalAmountByCategory(
                        totalAmount = it.get(amount).toLong(),
                        categoryId = MoneyUsageCategoryId(it.get(categories.MONEY_USAGE_CATEGORY_ID)!!),
                    )
                }
            }
        }
    }
}
