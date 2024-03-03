package net.matsudamper.money.backend.app.interfaces

import java.time.LocalDateTime
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId

interface MoneyUsageRepository {
    fun addMailRelation(userId: UserId, importedMailId: ImportedMailId, usageId: MoneyUsageId): Boolean
    fun addUsage(
        userId: UserId,
        title: String,
        description: String,
        subCategoryId: MoneyUsageSubCategoryId?,
        date: LocalDateTime,
        amount: Int,
    ): AddResult

    fun getMoneyUsageByQuery(
        userId: UserId,
        size: Int,
        cursor: GetMoneyUsageByQueryResult.Cursor?,
        isAsc: Boolean,
        sinceDateTime: LocalDateTime?,
        untilDateTime: LocalDateTime?,
        categoryIds: List<MoneyUsageCategoryId>,
        subCategoryIds: List<MoneyUsageSubCategoryId>,
        text: String?,
    ): GetMoneyUsageByQueryResult

    fun getMoneyUsage(userId: UserId, ids: List<MoneyUsageId>): Result<List<Usage>>
    fun getMails(userId: UserId, importedMailId: ImportedMailId): Result<List<Usage>>
    fun deleteUsage(userId: UserId, usageId: MoneyUsageId): Boolean
    fun updateUsage(userId: UserId, usageId: MoneyUsageId, title: String?, description: String?, subCategoryId: MoneyUsageSubCategoryId?, date: LocalDateTime?, amount: Int?): Boolean

    sealed interface AddResult {
        data class Success(val result: Usage) : AddResult
        data class Failed(val error: Throwable) : AddResult
    }

    data class Usage(
        val id: MoneyUsageId,
        val userId: UserId,
        val title: String,
        val description: String,
        val subCategoryId: MoneyUsageSubCategoryId?,
        val date: LocalDateTime,
        val amount: Int,
    )

    sealed interface GetMoneyUsageByQueryResult {
        data class Success(
            val ids: MutableList<MoneyUsageId>,
            val cursor: Cursor?,
        ) : GetMoneyUsageByQueryResult

        data class Failed(val error: Throwable) : GetMoneyUsageByQueryResult

        data class Cursor(
            val lastId: MoneyUsageId,
            val date: LocalDateTime,
        )
    }
}
