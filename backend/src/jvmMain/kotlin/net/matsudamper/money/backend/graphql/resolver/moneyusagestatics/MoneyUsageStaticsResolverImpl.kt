package net.matsudamper.money.backend.graphql.resolver.moneyusagestatics

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.MoneyUsageStaticsByCategoryResolver
import net.matsudamper.money.graphql.model.MoneyUsageStaticsBySubCategoryResolver
import net.matsudamper.money.graphql.model.MoneyUsageStaticsResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageStatics
import net.matsudamper.money.graphql.model.QlMoneyUsageStaticsByCategory
import java.util.concurrent.CompletionStage

class MoneyUsageStaticsResolverImpl: MoneyUsageStaticsResolver {
    override fun totalAmount(
        moneyUsageStatics: QlMoneyUsageStatics,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<Int>> {
        TODO("Not yet implemented")
    }

    override fun byCategories(
        moneyUsageStatics: QlMoneyUsageStatics,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsageStaticsByCategory>>> {
        TODO("Not yet implemented")
    }
}
