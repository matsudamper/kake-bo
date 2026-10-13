package net.matsudamper.money.categoryfilter

import net.matsudamper.money.element.MoneyUsageSubCategoryId

data class CategoryFilter(
    val orderNumber: Int,
    val operator: CategoryFilterOperator,
    val matchExpression: String?,
    val subCategoryId: MoneyUsageSubCategoryId?,
    val matchers: List<CategoryFilterMatcher>,
)

data class CategoryFilterMatcher(
    val matcherKey: String,
    val text: String,
    val dataSourceType: CategoryFilterDataSourceType,
    val matcherType: CategoryFilterMatcherType,
)
