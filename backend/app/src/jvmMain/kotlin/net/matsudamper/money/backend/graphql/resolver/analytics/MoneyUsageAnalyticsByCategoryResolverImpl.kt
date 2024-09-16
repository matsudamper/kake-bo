package net.matsudamper.money.backend.graphql.resolver.analytics

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.MoneyUsageAnalyticsBySubCategoryLoader
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageAnalyticsByCategoryLocalContext
import net.matsudamper.money.backend.graphql.requireLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.MoneyUsageAnalyticsByCategoryResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsByCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsBySubCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory

class MoneyUsageAnalyticsByCategoryResolverImpl : MoneyUsageAnalyticsByCategoryResolver {
    private val DataFetchingEnvironment.localContext: MoneyUsageAnalyticsByCategoryLocalContext
        get() = requireLocalContext()

    override fun bySubCategories(
        moneyUsageAnalyticsByCategory: QlMoneyUsageAnalyticsByCategory,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsageAnalyticsBySubCategory>?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()

        val dataLoader = context.dataLoaders.moneyUsageAnalyticsBySubCategoryLoader.get(env)
        val future =
            dataLoader.load(
                MoneyUsageAnalyticsBySubCategoryLoader.Key(
                    id = moneyUsageAnalyticsByCategory.category.id,
                    sinceDateTimeAt = env.localContext.query.sinceDateTime,
                    untilDateTimeAt = env.localContext.query.untilDateTime,
                ),
            )
        return CompletableFuture.allOf(future).thenApplyAsync {
            future.get().subCategories.map {
                QlMoneyUsageAnalyticsBySubCategory(
                    subCategory = QlMoneyUsageSubCategory(it.id),
                    totalAmount = it.totalAmount,
                )
            }
        }.toDataFetcher()
    }
}
