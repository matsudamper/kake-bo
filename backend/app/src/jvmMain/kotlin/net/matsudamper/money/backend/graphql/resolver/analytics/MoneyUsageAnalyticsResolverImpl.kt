package net.matsudamper.money.backend.graphql.resolver.analytics

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.DataFetcherResultBuilder
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageAnalyticsByCategoryLocalContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageAnalyticsLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.MoneyUsageAnalyticsResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalytics
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsByCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageCategory

class MoneyUsageAnalyticsResolverImpl : MoneyUsageAnalyticsResolver {
    private val DataFetchingEnvironment.localContext get() = getLocalContext<MoneyUsageAnalyticsLocalContext>()

    override fun totalAmount(
        moneyUsageAnalytics: QlMoneyUsageAnalytics,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Long?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            context.diContainer.createMoneyUsageAnalyticsRepository()
                .getTotalAmount(
                    userId = userId,
                    sinceDateTimeAt = env.localContext.query.sinceDateTime,
                    untilDateTimeAt = env.localContext.query.untilDateTime,
                )
        }.toDataFetcher()
    }

    override fun byCategories(
        moneyUsageAnalytics: QlMoneyUsageAnalytics,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsageAnalyticsByCategory>?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val results =
                context.diContainer.createMoneyUsageAnalyticsRepository()
                    .getTotalAmountByCategories(
                        userId = userId,
                        sinceDateTimeAt = env.localContext.query.sinceDateTime,
                        untilDateTimeAt = env.localContext.query.untilDateTime,
                    )
                    .onFailure {
                        it.printStackTrace()
                    }.getOrNull() ?: return@supplyAsync DataFetcherResultBuilder.buildNullValue()

            DataFetcherResultBuilder.nullable(
                value =
                    results.map {
                        QlMoneyUsageAnalyticsByCategory(
                            category = QlMoneyUsageCategory(it.categoryId),
                            totalAmount = it.totalAmount,
                        )
                    },
                localContext =
                    MoneyUsageAnalyticsByCategoryLocalContext(
                        query = env.localContext.query,
                    ),
            ).build()
        }
    }
}
