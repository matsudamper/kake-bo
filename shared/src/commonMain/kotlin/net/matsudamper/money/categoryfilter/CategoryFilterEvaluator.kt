package net.matsudamper.money.categoryfilter

import net.matsudamper.money.element.MoneyUsageSubCategoryId

fun evaluateCategoryFilters(
    filters: List<CategoryFilter>,
    dataExtractor: (CategoryFilterDataSourceType) -> String?,
): MoneyUsageSubCategoryId? {
    return filters
        .sortedBy { it.orderNumber }
        .firstOrNull { filter ->
            val conditions = filter.conditions.takeIf { it.isNotEmpty() } ?: return@firstOrNull false
            val results = conditions.asSequence().map { condition ->
                val targetText = dataExtractor(condition.dataSourceType) ?: return@map false
                when (condition.conditionType) {
                    CategoryFilterConditionType.Include -> targetText.contains(condition.text)
                    CategoryFilterConditionType.NotInclude -> !targetText.contains(condition.text)
                    CategoryFilterConditionType.Equal -> targetText == condition.text
                    CategoryFilterConditionType.NotEqual -> targetText != condition.text
                }
            }
            when (filter.operator) {
                CategoryFilterOperator.AND -> results.all { it }
                CategoryFilterOperator.OR -> results.any { it }
            }
        }
        ?.subCategoryId
}
