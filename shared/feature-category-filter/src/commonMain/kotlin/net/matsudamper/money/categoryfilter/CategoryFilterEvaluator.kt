package net.matsudamper.money.categoryfilter

import net.matsudamper.money.categoryfilter.matchexpression.MatchExpressionAnalysis
import net.matsudamper.money.categoryfilter.matchexpression.MatchExpressionAnalyzer
import net.matsudamper.money.element.MoneyUsageSubCategoryId

fun evaluateCategoryFilters(
    filters: List<CategoryFilter>,
    dataExtractor: (CategoryFilterDataSourceType) -> String?,
): MoneyUsageSubCategoryId? {
    return filters
        .sortedBy { it.orderNumber }
        .firstOrNull { filter -> isFilterMatched(filter, dataExtractor) }
        ?.subCategoryId
}

private fun isFilterMatched(
    filter: CategoryFilter,
    dataExtractor: (CategoryFilterDataSourceType) -> String?,
): Boolean {
    val matchExpression = filter.matchExpression
    return if (matchExpression.isNullOrBlank()) {
        isFilterMatchedByOperator(filter, dataExtractor)
    } else {
        isFilterMatchedByExpression(filter, matchExpression, dataExtractor)
    }
}

private fun isFilterMatchedByOperator(
    filter: CategoryFilter,
    dataExtractor: (CategoryFilterDataSourceType) -> String?,
): Boolean {
    val matchers = filter.matchers.takeIf { it.isNotEmpty() } ?: return false
    val results = matchers.asSequence().map { matcher -> isMatcherMatched(matcher, dataExtractor) }
    return when (filter.operator) {
        CategoryFilterOperator.AND -> results.all { it }
        CategoryFilterOperator.OR -> results.any { it }
    }
}

private fun isFilterMatchedByExpression(
    filter: CategoryFilter,
    matchExpression: String,
    dataExtractor: (CategoryFilterDataSourceType) -> String?,
): Boolean {
    val matchersByKey = filter.matchers.associateBy { it.matcherKey }
    return when (val analysis = MatchExpressionAnalyzer.analyze(matchExpression, matchersByKey.keys)) {
        is MatchExpressionAnalysis.Invalid -> false
        is MatchExpressionAnalysis.Valid -> analysis.expression.evaluate { matcherKey ->
            isMatcherMatched(matchersByKey.getValue(matcherKey), dataExtractor)
        }
    }
}

private fun isMatcherMatched(
    matcher: CategoryFilterMatcher,
    dataExtractor: (CategoryFilterDataSourceType) -> String?,
): Boolean {
    val targetText = dataExtractor(matcher.dataSourceType) ?: return false
    return when (matcher.matcherType) {
        CategoryFilterMatcherType.Include -> targetText.contains(matcher.text)
        CategoryFilterMatcherType.NotInclude -> !targetText.contains(matcher.text)
        CategoryFilterMatcherType.Equal -> targetText == matcher.text
        CategoryFilterMatcherType.NotEqual -> targetText != matcher.text
    }
}
