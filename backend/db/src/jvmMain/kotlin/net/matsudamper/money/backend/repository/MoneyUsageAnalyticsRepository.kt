package net.matsudamper.money.backend.repository

import java.time.LocalDateTime
import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JMoneyUsageCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.element.MoneyUsageCategoryId
import org.jooq.impl.DSL
import org.jooq.kotlin.and

class MoneyUsageAnalyticsRepository(
    private val dbConnection: DbConnection,
) {
    private val usage = JMoneyUsages.MONEY_USAGES
    private val categories = JMoneyUsageCategories.MONEY_USAGE_CATEGORIES
    private val subCategories = JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES

    fun getTotalAmount(
        userId: UserId,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Long? {
        return runCatching {
            dbConnection.use { connection ->
                val count = DSL.coalesce(DSL.sum(usage.AMOUNT), 0.toBigDecimal())
                val result = DSL.using(connection)
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

    fun getTotalAmountByCategories(
        userId: UserId,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Result<List<TotalAmountByCategory>> {
        return runCatching {
            dbConnection.use { connection ->
                val amount = DSL.sum(usage.AMOUNT)
                val result = DSL.using(connection)
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
                    TotalAmountByCategory(
                        totalAmount = it.get(amount).toLong(),
                        categoryId = MoneyUsageCategoryId(it.get(categories.MONEY_USAGE_CATEGORY_ID)!!),
                    )
                }
            }
        }
    }

    data class TotalAmountByCategory(
        val totalAmount: Long,
        val categoryId: MoneyUsageCategoryId,
    )
}
