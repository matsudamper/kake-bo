package net.matsudamper.money.frontend.android.feature.notificationusage

import com.apollographql.apollo.api.Optional
import net.matsudamper.money.categoryfilter.CategoryFilter
import net.matsudamper.money.categoryfilter.CategoryFilterDataSourceType
import net.matsudamper.money.categoryfilter.CategoryFilterMatcher
import net.matsudamper.money.categoryfilter.CategoryFilterMatcherType
import net.matsudamper.money.categoryfilter.CategoryFilterOperator
import net.matsudamper.money.categoryfilter.evaluateCategoryFilters
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.runCatchingWithoutCancel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.NotificationUsageCategoryFiltersQuery
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterMatcherType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersSortType
import net.matsudamper.money.frontend.graphql.type.ImportedMailFilterCategoryConditionOperator

internal interface NotificationUsageCategoryFilterRepository {
    suspend fun getMatchingSubCategoryId(title: String, serviceName: String): MoneyUsageSubCategoryId?
}

private const val TAG = "NotificationUsageCategoryFilterRepository"

internal class NotificationUsageCategoryFilterGraphqlRepository(
    private val graphqlClient: GraphqlClient,
) : NotificationUsageCategoryFilterRepository {
    override suspend fun getMatchingSubCategoryId(title: String, serviceName: String): MoneyUsageSubCategoryId? {
        val response = runCatchingWithoutCancel {
            graphqlClient.apolloClient
                .query(
                    NotificationUsageCategoryFiltersQuery(
                        query = ImportedMailCategoryFiltersQuery(
                            size = Optional.present(1000),
                            isAsc = true,
                            sortType = Optional.present(ImportedMailCategoryFiltersSortType.ORDER_NUMBER),
                        ),
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull() ?: return null

        val nodes = response.data?.user?.importedMailCategoryFilters?.nodes.orEmpty()
        val filters = nodes.map { node ->
            CategoryFilter(
                orderNumber = node.orderNumber,
                operator = node.operator.toShared(),
                matchExpression = node.matchExpression,
                subCategoryId = node.subCategory?.id,
                matchers = node.matchers.orEmpty().map { matcher ->
                    CategoryFilterMatcher(
                        matcherKey = matcher.matcherKey,
                        text = matcher.text,
                        dataSourceType = matcher.dataSourceType.toShared(),
                        matcherType = matcher.matcherType.toShared(),
                    )
                },
            )
        }
        return evaluateCategoryFilters(filters) { dataSourceType ->
            when (dataSourceType) {
                CategoryFilterDataSourceType.Title -> title
                CategoryFilterDataSourceType.ServiceName -> serviceName
                else -> null
            }
        }
    }

    private fun ImportedMailFilterCategoryConditionOperator.toShared(): CategoryFilterOperator {
        return when (this) {
            ImportedMailFilterCategoryConditionOperator.AND -> CategoryFilterOperator.AND
            ImportedMailFilterCategoryConditionOperator.OR -> CategoryFilterOperator.OR
            ImportedMailFilterCategoryConditionOperator.UNKNOWN__ -> CategoryFilterOperator.AND
        }
    }

    private fun ImportedMailCategoryFilterDataSourceType.toShared(): CategoryFilterDataSourceType {
        return when (this) {
            ImportedMailCategoryFilterDataSourceType.MailTitle -> CategoryFilterDataSourceType.MailTitle
            ImportedMailCategoryFilterDataSourceType.MailFrom -> CategoryFilterDataSourceType.MailFrom
            ImportedMailCategoryFilterDataSourceType.MailHtml -> CategoryFilterDataSourceType.MailHtml
            ImportedMailCategoryFilterDataSourceType.MailPlain -> CategoryFilterDataSourceType.MailPlain
            ImportedMailCategoryFilterDataSourceType.Title -> CategoryFilterDataSourceType.Title
            ImportedMailCategoryFilterDataSourceType.ServiceName -> CategoryFilterDataSourceType.ServiceName
            ImportedMailCategoryFilterDataSourceType.UNKNOWN__ -> CategoryFilterDataSourceType.Title
        }
    }

    private fun ImportedMailCategoryFilterMatcherType.toShared(): CategoryFilterMatcherType {
        return when (this) {
            ImportedMailCategoryFilterMatcherType.Include -> CategoryFilterMatcherType.Include
            ImportedMailCategoryFilterMatcherType.NotInclude -> CategoryFilterMatcherType.NotInclude
            ImportedMailCategoryFilterMatcherType.Equal -> CategoryFilterMatcherType.Equal
            ImportedMailCategoryFilterMatcherType.NotEqual -> CategoryFilterMatcherType.NotEqual
            ImportedMailCategoryFilterMatcherType.UNKNOWN__ -> CategoryFilterMatcherType.Include
        }
    }
}
