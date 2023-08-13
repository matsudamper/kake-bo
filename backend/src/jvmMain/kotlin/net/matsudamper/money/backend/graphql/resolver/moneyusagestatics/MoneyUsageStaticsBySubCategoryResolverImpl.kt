package net.matsudamper.money.backend.graphql.resolver.moneyusagestatics

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.MoneyUsageStaticsBySubCategoryResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageStaticsBySubCategory

class MoneyUsageStaticsBySubCategoryResolverImpl : MoneyUsageStaticsBySubCategoryResolver {
    override fun totalAmount(
        moneyUsageStaticsBySubCategory: QlMoneyUsageStaticsBySubCategory,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Long?>> {
        TODO("Not yet implemented")
    }
}
