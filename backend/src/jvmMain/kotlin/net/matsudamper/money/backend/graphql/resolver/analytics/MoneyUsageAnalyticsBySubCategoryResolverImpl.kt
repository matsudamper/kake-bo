package net.matsudamper.money.backend.graphql.resolver.analytics

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.MoneyUsageAnalyticsBySubCategoryResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsBySubCategory

class MoneyUsageAnalyticsBySubCategoryResolverImpl : MoneyUsageAnalyticsBySubCategoryResolver {
    override fun totalAmount(
        moneyUsageAnalyticsBySubCategory: QlMoneyUsageAnalyticsBySubCategory,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Long?>> {
        TODO("Not yet implemented")
    }
}
