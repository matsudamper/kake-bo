package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.ImportedMailDataLoaderDefine
import net.matsudamper.money.backend.dataloader.primeChildDataLoader
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageSuggestLocalContext
import net.matsudamper.money.backend.graphql.otelThenApplyAsync
import net.matsudamper.money.backend.graphql.requireLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.categoryfilter.CategoryFilterDataSourceType
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

            val sharedFilters = CategoryFilterFactory.create(
                filters = filtersFuture.get(),
                conditions = conditionsFuture.get(),
            )

            val subCategoryId = evaluateCategoryFilters(sharedFilters) { dataSourceType ->
                when (dataSourceType) {
                    CategoryFilterDataSourceType.MailTitle -> importedMail.subject
                    CategoryFilterDataSourceType.MailFrom -> importedMail.from
                    CategoryFilterDataSourceType.MailHtml -> importedMail.html
                    CategoryFilterDataSourceType.MailPlain -> importedMail.plain
                    CategoryFilterDataSourceType.Title -> moneyUsageSuggest.title
                    CategoryFilterDataSourceType.ServiceName -> moneyUsageSuggest.serviceName
                }
            }?.subCategoryId

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
