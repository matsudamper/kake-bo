package net.matsudamper.money.backend.app.interfaces

import java.time.LocalDateTime
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId

interface MoneyUsageAnalyticsRepository {
    fun getTotalAmount(
        userId: UserId,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Long?

    fun getTotalAmountBySubCategories(
        userId: UserId,
        categoryIds: List<MoneyUsageCategoryId>,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Result<List<TotalAmountBySubCategory>>

    fun getTotalAmountByCategories(
        userId: UserId,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Result<List<TotalAmountByCategory>>

    data class TotalAmountByCategory(
        val totalAmount: Long,
        val categoryId: MoneyUsageCategoryId,
    )

    data class TotalAmountBySubCategory(
        val categoryId: MoneyUsageCategoryId,
        val subCategories: List<SubCategoryTotalAmount>,
    )

    data class SubCategoryTotalAmount(
        val id: MoneyUsageSubCategoryId,
        val totalAmount: Long,
    )
}
