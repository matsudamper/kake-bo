package net.matsudamper.money.backend.graphql.resolver

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderDefine
import net.matsudamper.money.backend.dataloader.primeChildDataLoader
import net.matsudamper.money.backend.element.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.backend.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.element.ImportedMailFilterCategoryConditionOperator.AND
import net.matsudamper.money.backend.element.ImportedMailFilterCategoryConditionOperator.OR
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageSuggestLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.MoneyUsageSuggestResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSuggest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class MoneyUsageSuggestResolverImpl : MoneyUsageSuggestResolver {
    override fun subCategory(
        moneyUsageSuggest: QlMoneyUsageSuggest,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val localContext = env.getLocalContext<MoneyUsageSuggestLocalContext>()

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
        ).thenApplyAsync {
            val importedMail = importedMailFuture.get()
            val filters = filtersFuture.get().sortedBy { it.orderNumber }
            val conditionsMap = conditionsFuture.get().groupBy { it.filterId }

            val result = filters
                .firstOrNull { filter ->
                    val conditions = conditionsMap[filter.importedMailCategoryFilterId].orEmpty()
                        .takeIf { it.isNotEmpty() } ?: return@thenApplyAsync null

                    val results = conditions.asSequence().map { condition ->
                        val targetText = when (condition.dataSourceType) {
                            ImportedMailCategoryFilterDatasourceType.MailTitle -> importedMail.subject
                            ImportedMailCategoryFilterDatasourceType.MailFrom -> importedMail.from
                            ImportedMailCategoryFilterDatasourceType.MailHTML -> importedMail.html
                            ImportedMailCategoryFilterDatasourceType.MailPlain -> importedMail.plain
                            ImportedMailCategoryFilterDatasourceType.Title -> moneyUsageSuggest.title
                            ImportedMailCategoryFilterDatasourceType.ServiceName -> moneyUsageSuggest.serviceName
                        }.orEmpty()

                        when (condition.conditionType) {
                            ImportedMailCategoryFilterConditionType.Include -> {
                                targetText.contains(condition.text)
                            }

                            ImportedMailCategoryFilterConditionType.NotInclude -> {
                                targetText.contains(condition.text).not()
                            }

                            ImportedMailCategoryFilterConditionType.Equal -> {
                                targetText == condition.text
                            }

                            ImportedMailCategoryFilterConditionType.NotEqual -> {
                                targetText != condition.text
                            }
                        }
                    }

                    when (filter.operator) {
                        AND -> results.all { it }
                        OR -> results.any { it }
                    }
                }

            val subCategoryId = result?.moneyUsageSubCategoryId
            if (subCategoryId == null) {
                null
            } else {
                QlMoneyUsageSubCategory(
                    id = subCategoryId,
                )
            }
        }.toDataFetcher()
    }
}
