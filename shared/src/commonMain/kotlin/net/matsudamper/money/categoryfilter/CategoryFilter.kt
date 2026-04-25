package net.matsudamper.money.categoryfilter

import net.matsudamper.money.element.MoneyUsageSubCategoryId

data class CategoryFilter(
    val orderNumber: Int,
    val operator: CategoryFilterOperator,
    val subCategoryId: MoneyUsageSubCategoryId?,
    val conditions: List<CategoryFilterCondition>,
)

data class CategoryFilterCondition(
    val text: String,
    val dataSourceType: CategoryFilterDataSourceType,
    val conditionType: CategoryFilterConditionType,
)
