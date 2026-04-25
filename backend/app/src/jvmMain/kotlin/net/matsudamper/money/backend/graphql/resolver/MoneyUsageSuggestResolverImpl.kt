package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderDefine
import net.matsudamper.money.backend.dataloader.primeChildDataLoader
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageSuggestLocalContext
import net.matsudamper.money.backend.graphql.otelThenApplyAsync
import net.matsudamper.money.backend.graphql.requireLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.categoryfilter.CategoryFilter
import net.matsudamper.money.categoryfilter.CategoryFilterCondition
import net.matsudamper.money.categoryfilter.CategoryFilterConditionType
import net.matsudamper.money.categoryfilter.CategoryFilterDataSourceType
import net.matsudamper.money.categoryfilter.CategoryFilterOperator
import net.matsudamper.money.categoryfilter.evaluateCategoryFilters
import net.matsudamper.money.graphql.model.MoneyUsageSuggestResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSuggest

class MoneyUsageSuggestResolverImpl : MoneyUsageSuggestResolver {
    override fun subCategory(
        moneyUsageSuggest: QlMoneyUsageSuggest,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val localContext = env.requireLocalContext<MoneyUsageSuggestLocalContext>()

        val importedMailFuture = context.dataLoaders.importedMailDataLoader.get(env).load(
            ImportedMailDataLoaderDefine.Key(
                userId = userId,
                importedMailId = localContext.importedMailId,
            ),
        )

        val filtersFuture = context.dataLoaders.importedMailCategoryFiltersDataLoader.get(env)
            .load(userId)
            .primeChildDataLoader(env)

        val conditionsFuture = context.dataLoaders.importedMailCategoryFilterConditionsDataLoader.get(env)
            .load(userId)
            .primeChildDataLoader(env)

        return CompletableFuture.allOf(
            importedMailFuture,
            filtersFuture,
            conditionsFuture,
        ).otelThenApplyAsync {
            val importedMail = importedMailFuture.get()
            val filters = filtersFuture.get()
            val conditionsMap = conditionsFuture.get().groupBy { it.filterId }

            val sharedFilters = filters.map { filter ->
                CategoryFilter(
                    orderNumber = filter.orderNumber,
                    operator = filter.operator.toShared(),
                    subCategoryId = filter.moneyUsageSubCategoryId,
                    conditions = conditionsMap[filter.importedMailCategoryFilterId].orEmpty().map { c ->
                        CategoryFilterCondition(
                            text = c.text,
                            dataSourceType = c.dataSourceType.toShared(),
                            conditionType = c.conditionType.toShared(),
                        )
                    },
                )
            }

            val subCategoryId = evaluateCategoryFilters(sharedFilters) { dataSourceType ->
                when (dataSourceType) {
                    CategoryFilterDataSourceType.MailTitle -> importedMail.subject
                    CategoryFilterDataSourceType.MailFrom -> importedMail.from
                    CategoryFilterDataSourceType.MailHtml -> importedMail.html
                    CategoryFilterDataSourceType.MailPlain -> importedMail.plain
                    CategoryFilterDataSourceType.Title -> moneyUsageSuggest.title
                    CategoryFilterDataSourceType.ServiceName -> moneyUsageSuggest.serviceName
                }
            }

            if (subCategoryId == null) {
                null
            } else {
                QlMoneyUsageSubCategory(
                    id = subCategoryId,
                )
            }
        }.toDataFetcher()
    }

    private fun ImportedMailFilterCategoryConditionOperator.toShared(): CategoryFilterOperator {
        return when (this) {
            ImportedMailFilterCategoryConditionOperator.AND -> CategoryFilterOperator.AND
            ImportedMailFilterCategoryConditionOperator.OR -> CategoryFilterOperator.OR
        }
    }

    private fun ImportedMailCategoryFilterDatasourceType.toShared(): CategoryFilterDataSourceType {
        return when (this) {
            ImportedMailCategoryFilterDatasourceType.MailTitle -> CategoryFilterDataSourceType.MailTitle
            ImportedMailCategoryFilterDatasourceType.MailFrom -> CategoryFilterDataSourceType.MailFrom
            ImportedMailCategoryFilterDatasourceType.MailHTML -> CategoryFilterDataSourceType.MailHtml
            ImportedMailCategoryFilterDatasourceType.MailPlain -> CategoryFilterDataSourceType.MailPlain
            ImportedMailCategoryFilterDatasourceType.Title -> CategoryFilterDataSourceType.Title
            ImportedMailCategoryFilterDatasourceType.ServiceName -> CategoryFilterDataSourceType.ServiceName
        }
    }

    private fun ImportedMailCategoryFilterConditionType.toShared(): CategoryFilterConditionType {
        return when (this) {
            ImportedMailCategoryFilterConditionType.Include -> CategoryFilterConditionType.Include
            ImportedMailCategoryFilterConditionType.NotInclude -> CategoryFilterConditionType.NotInclude
            ImportedMailCategoryFilterConditionType.Equal -> CategoryFilterConditionType.Equal
            ImportedMailCategoryFilterConditionType.NotEqual -> CategoryFilterConditionType.NotEqual
        }
    }
}
