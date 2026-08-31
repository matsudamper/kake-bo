package net.matsudamper.money.categoryfilter

fun evaluateCategoryFilters(
    filters: List<CategoryFilter>,
    dataExtractor: (CategoryFilterDataSourceType) -> String?,
): CategoryFilter? {
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
}

fun appendCategoryFilterDescription(
    description: String,
    descriptionSuffix: String?,
): String {
    return when {
        descriptionSuffix.isNullOrEmpty() -> description
        description.isEmpty() -> descriptionSuffix
        // 元の説明と繋がって読めなくならないように改行で区切る
        else -> "$description\n$descriptionSuffix"
    }
}
