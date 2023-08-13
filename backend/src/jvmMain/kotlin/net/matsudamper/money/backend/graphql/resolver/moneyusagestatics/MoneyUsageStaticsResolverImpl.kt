package net.matsudamper.money.backend.graphql.resolver.moneyusagestatics

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageStaticsLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.MoneyUsageStaticsResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageStatics
import net.matsudamper.money.graphql.model.QlMoneyUsageStaticsByCategory

class MoneyUsageStaticsResolverImpl : MoneyUsageStaticsResolver {
    private val DataFetchingEnvironment.localContext get() = getLocalContext<MoneyUsageStaticsLocalContext>()

    override fun totalAmount(
        moneyUsageStatics: QlMoneyUsageStatics,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Long?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            context.repositoryFactory.createMoneyUsageStaticsRepository()
                .getTotalAmount(
                    userId = userId,
                    sinceDateTimeAt = env.localContext.query.sinceDateTime,
                    untilDateTimeAt = env.localContext.query.untilDateTime,
                )
        }.toDataFetcher()
    }

    override fun byCategories(
        moneyUsageStatics: QlMoneyUsageStatics,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsageStaticsByCategory>>> {
        TODO("Not yet implemented")
    }
}
