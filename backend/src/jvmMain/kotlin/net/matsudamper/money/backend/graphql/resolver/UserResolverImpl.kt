package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.repository.MoneyUsageSubCategoryRepository
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.graphql.model.QlMoneyUsageCategoriesConnection
import net.matsudamper.money.graphql.model.QlMoneyUsageCategoriesInput
import net.matsudamper.money.graphql.model.QlMoneyUsageCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageCategoryInput
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategoriesFromCategoryIdConnection
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategoriesFromCategoryIdQuery
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategoryInput
import net.matsudamper.money.graphql.model.QlUser
import net.matsudamper.money.graphql.model.QlUserSettings
import net.matsudamper.money.graphql.model.UserResolver

class UserResolverImpl : UserResolver {
    override fun settings(user: QlUser, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUserSettings>> {
        return CompletableFuture.completedFuture(QlUserSettings()).toDataFetcher()
    }

    /**
     * TODO: Paging
     */
    override fun moneyUsageCategories(
        user: QlUser,
        input: QlMoneyUsageCategoriesInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageCategoriesConnection?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val result = context.repositoryFactory.createMoneyUsageCategoryRepository()
                .getCategory(userId = userId)

            return@supplyAsync when (result) {
                is MoneyUsageCategoryRepository.GetCategoryResult.Failed -> {
                    null
                }

                is MoneyUsageCategoryRepository.GetCategoryResult.Success -> {
                    QlMoneyUsageCategoriesConnection(
                        nodes = result.results.map {
                            QlMoneyUsageCategory(
                                id = it.moneyUsageCategoryId,
                            )
                        },
                        cursor = null, // TODO
                    )
                }
            }
        }.toDataFetcher()
    }

    override fun moneyUsageCategory(user: QlUser, input: QlMoneyUsageCategoryInput, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlMoneyUsageCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()

        return CompletableFuture.completedFuture(
            QlMoneyUsageCategory(id = input.id),
        ).toDataFetcher()
    }

    override fun moneyUsageSubCategory(user: QlUser, input: QlMoneyUsageSubCategoryInput, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()

        return CompletableFuture.completedFuture(
            QlMoneyUsageSubCategory(id = input.id),
        ).toDataFetcher()
    }

    override fun moneyUsageSubCategoriesFromCategoryId(
        user: QlUser,
        input: QlMoneyUsageSubCategoriesFromCategoryIdQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategoriesFromCategoryIdConnection?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val result = context.repositoryFactory.createMoneyUsageSubCategoryRepository()
                .getSubCategory(userId = userId, categoryId = input.id)

            QlMoneyUsageSubCategoriesFromCategoryIdConnection(
                nodes = when (result) {
                    is MoneyUsageSubCategoryRepository.GetSubCategoryResult.Failed -> {
                        result.e.printStackTrace()
                        return@supplyAsync null
                    }

                    is MoneyUsageSubCategoryRepository.GetSubCategoryResult.Success -> {
                        result.results.map { QlMoneyUsageSubCategory(it.moneyUsageSubCategoryId) }
                    }
                },
                cursor = null,
            )
        }.toDataFetcher()
    }
}
