package net.matsudamper.money.backend.graphql.resolver.moneyusagestatics

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.MoneyUsageStaticsByCategoryResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageStaticsByCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageStaticsBySubCategory

class MoneyUsageStaticsByCategoryResolverImpl : MoneyUsageStaticsByCategoryResolver {
    override fun bySubCategories(
        moneyUsageStaticsByCategory: QlMoneyUsageStaticsByCategory,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlMoneyUsageStaticsBySubCategory>?>> {
        TODO("Not yet implemented")
    }
}
