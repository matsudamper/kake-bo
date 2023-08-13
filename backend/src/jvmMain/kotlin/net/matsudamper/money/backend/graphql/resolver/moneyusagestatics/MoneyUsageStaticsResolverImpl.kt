package net.matsudamper.money.backend.graphql.resolver.moneyusagestatics

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.MoneyUsageStaticsResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageStatics
import net.matsudamper.money.graphql.model.QlMoneyUsageStaticsByCategory

class MoneyUsageStaticsResolverImpl : MoneyUsageStaticsResolver {
    override fun totalAmount(
        moneyUsageStatics: QlMoneyUsageStatics,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int>> {
        TODO("Not yet implemented")
    }

    override fun byCategories(
        moneyUsageStatics: QlMoneyUsageStatics,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsageStaticsByCategory>>> {
        TODO("Not yet implemented")
    }
}
