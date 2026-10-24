package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterMatcherType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.ImportedMailCategoryFilterMatcherId
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
        size: Int,
    ): Result<MailFiltersResult>

    fun getMatchers(
        userId: UserId,
        filterId: ImportedMailCategoryFilterId,
    ): Result<MailFilterMatcherResult>

    data class MailFiltersResult(
        val items: List<MailFilter>,
        val cursor: MailFilterCursor?,
    )

    data class MailFilterMatcherResult(
        val filterId: ImportedMailCategoryFilterId,
        val matchers: List<Matcher>,
    )

    data class MailFilter(
        val importedMailCategoryFilterId: ImportedMailCategoryFilterId,
        val userId: UserId,
        val title: String,
        val moneyUsageSubCategoryId: MoneyUsageSubCategoryId?,
        val operator: ImportedMailFilterCategoryConditionOperator,
        val matchExpression: String?,
        val orderNumber: Int,
    )

    data class Matcher(
        val filterId: ImportedMailCategoryFilterId,
        val matcherId: ImportedMailCategoryFilterMatcherId,
        val matcherKey: String,
        val text: String,
        val matcherType: ImportedMailCategoryFilterMatcherType,
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

    fun getMatchers(
        userId: UserId,
        matcherIds: List<ImportedMailCategoryFilterMatcherId>,
    ): Result<List<Matcher>>

    fun updateFilter(
        filterId: ImportedMailCategoryFilterId,
        userId: UserId,
        title: String? = null,
        orderNum: Int? = null,
        subCategory: MoneyUsageSubCategoryId? = null,
        operator: ImportedMailFilterCategoryConditionOperator? = null,
        matchExpression: UpdateValue<String?> = UpdateValue.NotUpdate,
    ): Boolean

    fun deleteFilter(
        filterId: ImportedMailCategoryFilterId,
        userId: UserId,
    ): Boolean

    fun addMatcher(
        userId: UserId,
        filterId: ImportedMailCategoryFilterId,
        matcherType: ImportedMailCategoryFilterMatcherType?,
        text: String?,
        dataSource: ImportedMailCategoryFilterDatasourceType?,
    ): Boolean

    fun updateMatcher(
        userId: UserId,
        matcherId: ImportedMailCategoryFilterMatcherId,
        matcherKey: String?,
        text: String?,
        matcherType: ImportedMailCategoryFilterMatcherType?,
        dataSource: ImportedMailCategoryFilterDatasourceType?,
    ): Boolean

    fun deleteMatcher(
        userId: UserId,
        matcherId: ImportedMailCategoryFilterMatcherId,
    ): Boolean

    fun getFilters(userId: UserId): List<MailFilter>

    fun getMatchers(userId: UserId): List<Matcher>
}
