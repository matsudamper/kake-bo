package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.MoneyUsageSubCategoryDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.MoneyUsageSubCategoryResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory

class MoneyUsageSubCategoryResolverImpl : MoneyUsageSubCategoryResolver {
    override fun name(
        moneyUsageSubCategory: QlMoneyUsageSubCategory,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return context.dataLoaders.moneyUsageSubCategoryDataLoader.get(env)
            .load(
                MoneyUsageSubCategoryDataLoaderDefine.Key(
                    userId = userId,
                    subCategoryId = moneyUsageSubCategory.id,
                ),
            ).thenApplyAsync { subCategoryLoader ->
                subCategoryLoader!!.name
            }.toDataFetcher()
    }

    override fun category(
        moneyUsageSubCategory: QlMoneyUsageSubCategory,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageCategory>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return context.dataLoaders.moneyUsageSubCategoryDataLoader.get(env)
            .load(
                MoneyUsageSubCategoryDataLoaderDefine.Key(
                    userId = userId,
                    subCategoryId = moneyUsageSubCategory.id,
                ),
            ).thenApplyAsync { subCategoryLoader ->
                QlMoneyUsageCategory(
                    id = subCategoryLoader!!.categoryId,
                )
            }.toDataFetcher()
    }
}
