package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId

interface MailFilterRepository {
    fun addFilter(
        title: String,
        userId: UserId,
        orderNum: Int,
    ): Result<MailFilter>

    fun getFilters(
        userId: UserId,
        categoryFilterIds: List<ImportedMailCategoryFilterId>,
    ): Result<List<MailFilter>>

    fun getFilters(
        isAsc: Boolean,
        sortType: SortType,
        userId: UserId,
        cursor: MailFilterCursor?,
    ): Result<MailFiltersResult>

    fun getConditions(
        userId: UserId,
        filterId: ImportedMailCategoryFilterId,
    ): Result<MailFilterConditionResult>

    data class MailFiltersResult(
        val items: List<MailFilter>,
        val cursor: MailFilterCursor?,
    )

    data class MailFilterConditionResult(
        val filterId: ImportedMailCategoryFilterId,
        val conditions: List<Condition>,
    )

    data class MailFilter(
        val importedMailCategoryFilterId: ImportedMailCategoryFilterId,
        val userId: UserId,
        val title: String,
        val moneyUsageSubCategoryId: MoneyUsageSubCategoryId?,
        val operator: ImportedMailFilterCategoryConditionOperator,
        val orderNumber: Int,
    )

    data class Condition(
        val filterId: ImportedMailCategoryFilterId,
        val conditionId: ImportedMailCategoryFilterConditionId,
        val text: String,
        val conditionType: ImportedMailCategoryFilterConditionType,
        val dataSourceType: ImportedMailCategoryFilterDatasourceType,
    )

    data class MailFilterCursor(
        val id: ImportedMailCategoryFilterId,
        val title: String,
        val orderNumber: Int,
    )

    enum class SortType {
        TITLE,
        ORDER_NUMBER,
    }

    fun getConditions(
        userId: UserId,
        filterIds: List<ImportedMailCategoryFilterConditionId>,
    ): Result<List<Condition>>

    fun updateFilter(
        filterId: ImportedMailCategoryFilterId,
        userId: UserId,
        title: String? = null,
        orderNum: Int? = null,
        subCategory: MoneyUsageSubCategoryId? = null,
        operator: ImportedMailFilterCategoryConditionOperator? = null,
    ): Boolean

    fun deleteFilter(
        filterId: ImportedMailCategoryFilterId,
        userId: UserId,
    ): Boolean

    fun addCondition(
        userId: UserId,
        filterId: ImportedMailCategoryFilterId,
        condition: ImportedMailCategoryFilterConditionType?,
        text: String?,
        dataSource: ImportedMailCategoryFilterDatasourceType?,
    ): Boolean

    fun updateCondition(
        userId: UserId,
        conditionId: ImportedMailCategoryFilterConditionId,
        text: String?,
        conditionType: ImportedMailCategoryFilterConditionType?,
        dataSource: ImportedMailCategoryFilterDatasourceType?,
    ): Boolean

    fun deleteCondition(
        userId: UserId,
        conditionId: ImportedMailCategoryFilterConditionId,
    ): Boolean

    fun getFilters(userId: UserId): List<MailFilter>

    fun getConditions(userId: UserId): List<Condition>
}
