package net.matsudamper.money.frontend.android.feature.notificationusage

import com.apollographql.apollo.api.Optional
import net.matsudamper.money.categoryfilter.CategoryFilter
import net.matsudamper.money.categoryfilter.CategoryFilterCondition
import net.matsudamper.money.categoryfilter.CategoryFilterConditionType
import net.matsudamper.money.categoryfilter.CategoryFilterDataSourceType
import net.matsudamper.money.categoryfilter.CategoryFilterOperator
import net.matsudamper.money.categoryfilter.evaluateCategoryFilters
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.runCatchingWithoutCancel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.NotificationUsageCategoryFiltersQuery
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFilterDataSourceType
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersSortType
import net.matsudamper.money.frontend.graphql.type.ImportedMailFilterCategoryConditionOperator

internal interface NotificationUsageCategoryFilterRepository {
    suspend fun getMatchingSubCategoryId(title: String, serviceName: String): MoneyUsageSubCategoryId?
}

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
        }.getOrNull() ?: return null

        val nodes = response.data?.user?.importedMailCategoryFilters?.nodes.orEmpty()
        val filters = nodes.map { node ->
            CategoryFilter(
                orderNumber = node.orderNumber,
                operator = node.operator.toShared(),
                subCategoryId = node.subCategory?.id,
                conditions = node.conditions.orEmpty().map { c ->
                    CategoryFilterCondition(
                        text = c.text,
                        dataSourceType = c.dataSourceType.toShared(),
                        conditionType = c.conditionType.toShared(),
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

    private fun ImportedMailCategoryFilterConditionType.toShared(): CategoryFilterConditionType {
        return when (this) {
            ImportedMailCategoryFilterConditionType.Include -> CategoryFilterConditionType.Include
            ImportedMailCategoryFilterConditionType.NotInclude -> CategoryFilterConditionType.NotInclude
            ImportedMailCategoryFilterConditionType.Equal -> CategoryFilterConditionType.Equal
            ImportedMailCategoryFilterConditionType.NotEqual -> CategoryFilterConditionType.NotEqual
            ImportedMailCategoryFilterConditionType.UNKNOWN__ -> CategoryFilterConditionType.Include
        }
    }
}
