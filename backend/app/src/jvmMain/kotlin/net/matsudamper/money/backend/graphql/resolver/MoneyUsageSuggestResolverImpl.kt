package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterMatcherType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderDefine
import net.matsudamper.money.backend.dataloader.primeChildDataLoader
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageSuggestLocalContext
import net.matsudamper.money.backend.graphql.otelThenApplyAsync
import net.matsudamper.money.backend.graphql.requireLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.categoryfilter.CategoryFilter
import net.matsudamper.money.categoryfilter.CategoryFilterDataSourceType
import net.matsudamper.money.categoryfilter.CategoryFilterMatcher
import net.matsudamper.money.categoryfilter.CategoryFilterMatcherType
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

        val matchersFuture = context.dataLoaders.importedMailCategoryFilterMatchersDataLoader.get(env)
            .load(userId)
            .primeChildDataLoader(env)

        return CompletableFuture.allOf(
            importedMailFuture,
            filtersFuture,
            matchersFuture,
        ).otelThenApplyAsync {
            val importedMail = importedMailFuture.get()
            val filters = filtersFuture.get()
            val matchersMap = matchersFuture.get().groupBy { it.filterId }

            val sharedFilters = filters.map { filter ->
                CategoryFilter(
                    orderNumber = filter.orderNumber,
                    operator = filter.operator.toShared(),
                    matchExpression = filter.matchExpression,
                    subCategoryId = filter.moneyUsageSubCategoryId,
                    matchers = matchersMap[filter.importedMailCategoryFilterId].orEmpty().map { matcher ->
                        CategoryFilterMatcher(
                            matcherKey = matcher.matcherKey,
                            text = matcher.text,
                            dataSourceType = matcher.dataSourceType.toShared(),
                            matcherType = matcher.matcherType.toShared(),
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

    private fun ImportedMailCategoryFilterMatcherType.toShared(): CategoryFilterMatcherType {
        return when (this) {
            ImportedMailCategoryFilterMatcherType.Include -> CategoryFilterMatcherType.Include
            ImportedMailCategoryFilterMatcherType.NotInclude -> CategoryFilterMatcherType.NotInclude
            ImportedMailCategoryFilterMatcherType.Equal -> CategoryFilterMatcherType.Equal
            ImportedMailCategoryFilterMatcherType.NotEqual -> CategoryFilterMatcherType.NotEqual
        }
    }
}
