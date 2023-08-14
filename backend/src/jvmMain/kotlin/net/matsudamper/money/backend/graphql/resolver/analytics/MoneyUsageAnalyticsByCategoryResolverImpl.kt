package net.matsudamper.money.backend.graphql.resolver.analytics

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.MoneyUsageAnalyticsByCategoryResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsByCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsBySubCategory

class MoneyUsageAnalyticsByCategoryResolverImpl : MoneyUsageAnalyticsByCategoryResolver {
    override fun bySubCategories(
        moneyUsageAnalyticsByCategory: QlMoneyUsageAnalyticsByCategory,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsageAnalyticsBySubCategory>?>> {
        TODO("Not yet implemented")
    }
}
