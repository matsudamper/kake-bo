package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.UserId

interface MoneyUsageCategoryRepository {
    fun addCategory(
        userId: UserId,
        name: String,
    ): AddCategoryResult

    fun getCategory(
        userId: UserId,
        moneyUsageCategoryIds: List<MoneyUsageCategoryId>,
    ): GetCategoryResult

    fun getCategory(userId: UserId): GetCategoryResult

    fun updateCategory(
        userId: UserId,
        categoryId: MoneyUsageCategoryId,
        name: String?,
    ): Boolean

    data class CategoryResult(
        val userId: UserId,
        val moneyUsageCategoryId: MoneyUsageCategoryId,
        val name: String,
    )

    sealed interface AddCategoryResult {
        data class Success(val result: CategoryResult) : AddCategoryResult

        data class Failed(val error: Throwable) : AddCategoryResult
    }

    sealed interface GetCategoryResult {
        data class Success(val results: List<CategoryResult>) : GetCategoryResult

        data class Failed(val e: Throwable) : GetCategoryResult
    }
}
