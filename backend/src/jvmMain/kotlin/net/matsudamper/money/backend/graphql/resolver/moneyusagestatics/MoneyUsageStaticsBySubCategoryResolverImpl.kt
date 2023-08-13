package net.matsudamper.money.backend.graphql.resolver.moneyusagestatics

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.MoneyUsageStaticsBySubCategoryResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageStaticsBySubCategory
import java.util.concurrent.CompletionStage

class MoneyUsageStaticsBySubCategoryResolverImpl: MoneyUsageStaticsBySubCategoryResolver {
    override fun totalAmount(
        moneyUsageStaticsBySubCategory: QlMoneyUsageStaticsBySubCategory,
        env: DataFetchingEnvironment
    ): CompletionStage<DataFetcherResult<Int>> {
        TODO("Not yet implemented")
    }

}
