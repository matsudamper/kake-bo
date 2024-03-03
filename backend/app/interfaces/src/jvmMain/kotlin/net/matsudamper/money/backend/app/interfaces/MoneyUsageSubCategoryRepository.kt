package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId

interface MoneyUsageSubCategoryRepository {
    fun addSubCategory(
        userId: UserId,
        name: String,
        categoryId: MoneyUsageCategoryId,
    ): AddSubCategoryResult

    fun getSubCategory(
        userId: UserId,
        categoryId: MoneyUsageCategoryId,
    ): GetSubCategoryResult

    fun getSubCategory(
        userId: UserId,
        moneyUsageSubCategoryIds: List<MoneyUsageSubCategoryId>,
    ): GetSubCategoryResult

    fun updateSubCategory(
        userId: UserId,
        subCategoryId: MoneyUsageSubCategoryId,
        name: String?,
    ): Boolean

    fun deleteSubCategory(
        userId: UserId,
        subCategoryId: MoneyUsageSubCategoryId,
    ): Boolean

    data class SubCategoryResult(
        val userId: UserId,
        val moneyUsageCategoryId: MoneyUsageCategoryId,
        val moneyUsageSubCategoryId: MoneyUsageSubCategoryId,
        val name: String,
    )

    sealed interface AddSubCategoryResult {
        data class Success(val result: SubCategoryResult) : AddSubCategoryResult

        sealed interface Failed : AddSubCategoryResult {
            object CategoryNotFound : AddSubCategoryResult

            data class Error(val error: Throwable) : AddSubCategoryResult
        }
    }

    sealed interface GetSubCategoryResult {
        data class Success(val results: List<SubCategoryResult>) : GetSubCategoryResult

        data class Failed(val e: Throwable) : GetSubCategoryResult
    }
}
