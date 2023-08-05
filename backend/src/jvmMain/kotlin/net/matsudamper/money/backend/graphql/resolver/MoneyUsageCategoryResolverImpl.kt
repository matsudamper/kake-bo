package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.MoneyUsageCategoryDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.MoneyUsageSubCategoryRepository
import net.matsudamper.money.graphql.model.MoneyUsageCategoryResolver
import net.matsudamper.money.graphql.model.QlMoneyUsageCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategoryQuery
import net.matsudamper.money.graphql.model.QlSubCategoriesConnection
import org.dataloader.impl.CompletableFutureKit

class MoneyUsageCategoryResolverImpl : MoneyUsageCategoryResolver {
    override fun name(
        moneyUsageCategory: QlMoneyUsageCategory,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        val categoryLoader = context.dataLoaders.moneyUsageCategoryDataLoaderDefine.get(env)
            .load(
                MoneyUsageCategoryDataLoaderDefine.Key(
                    userId = userId,
                    categoryId = moneyUsageCategory.id,
                ),
            )

        return CompletableFuture.allOf(categoryLoader).thenApplyAsync {
            categoryLoader.get()!!.name
        }.toDataFetcher()
    }

    /**
     * TODO: Paging
     */
    override fun subCategories(
        moneyUsageCategory: QlMoneyUsageCategory,
        query: QlMoneyUsageSubCategoryQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlSubCategoriesConnection?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val result = context.repositoryFactory.createMoneyUsageSubCategoryRepository()
                .getSubCategory(
                    userId = userId,
                    categoryId = moneyUsageCategory.id,
                )
            when (result) {
                is MoneyUsageSubCategoryRepository.GetSubCategoryResult.Failed -> {
                    result.e.printStackTrace()
                    null
                }

                is MoneyUsageSubCategoryRepository.GetSubCategoryResult.Success -> {
                    QlSubCategoriesConnection(
                        nodes = result.results.map {
                            QlMoneyUsageSubCategory(
                                id = it.moneyUsageSubCategoryId,
                            )
                        },
                        cursor = null, // TODO
                    )
                }
            }
        }.toDataFetcher()
    }
}
