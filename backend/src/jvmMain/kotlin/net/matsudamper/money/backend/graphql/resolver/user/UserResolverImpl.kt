package net.matsudamper.money.backend.graphql.resolver.user

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.dataloader.MoneyUsageDataLoaderDefine
import net.matsudamper.money.backend.graphql.DataFetcherResultBuilder
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageAnalyticsByCategoryLocalContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageAnalyticsLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.repository.MoneyUsageRepository
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.graphql.model.QlFidoInfo
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilter
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFiltersConnection
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFiltersQuery
import net.matsudamper.money.graphql.model.QlMoneyUsage
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalytics
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsByCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsQuery
import net.matsudamper.money.graphql.model.QlMoneyUsageCategoriesConnection
import net.matsudamper.money.graphql.model.QlMoneyUsageCategoriesInput
import net.matsudamper.money.graphql.model.QlMoneyUsageCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategoryInput
import net.matsudamper.money.graphql.model.QlMoneyUsagesConnection
import net.matsudamper.money.graphql.model.QlMoneyUsagesQuery
import net.matsudamper.money.graphql.model.QlUser
import net.matsudamper.money.graphql.model.QlUserSettings
import net.matsudamper.money.graphql.model.UserResolver

class UserResolverImpl : UserResolver {
    override fun settings(
        user: QlUser,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlUserSettings>> {
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

    override fun moneyUsageCategory(
        user: QlUser,
        id: MoneyUsageCategoryId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()

        return CompletableFuture.completedFuture(
            QlMoneyUsageCategory(id = id),
        ).toDataFetcher()
    }

    override fun moneyUsageSubCategory(
        user: QlUser,
        input: QlMoneyUsageSubCategoryInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageSubCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()

        return CompletableFuture.completedFuture(
            QlMoneyUsageSubCategory(id = input.id),
        ).toDataFetcher()
    }

    override fun moneyUsages(
        user: QlUser,
        query: QlMoneyUsagesQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsagesConnection?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val results = context.repositoryFactory.createMoneyUsageRepository()
                .getMoneyUsageByQuery(
                    userId = userId,
                    size = query.size,
                    isAsc = query.isAsc,
                    cursor = query.cursor?.let { MoneyUsagesCursor.fromString(it) }?.let {
                        MoneyUsageRepository.GetMoneyUsageByQueryResult.Cursor(
                            lastId = it.lastId,
                            date = it.lastDate,
                        )
                    },
                    sinceDateTime = query.filter?.sinceDateTime,
                    untilDateTime = query.filter?.untilDateTime,
                    categoryIds = query.filter?.category.orEmpty(),
                    subCategoryIds = query.filter?.subCategory.orEmpty(),
                )
            val result = when (results) {
                is MoneyUsageRepository.GetMoneyUsageByQueryResult.Failed -> throw results.error
                is MoneyUsageRepository.GetMoneyUsageByQueryResult.Success -> results
            }
            QlMoneyUsagesConnection(
                nodes = result.ids.map { id ->
                    QlMoneyUsage(
                        id = id,
                    )
                },
                cursor = result.cursor?.let { cursor ->
                    MoneyUsagesCursor(
                        lastId = cursor.lastId,
                        lastDate = cursor.date,
                    ).toCursorString()
                },
                hasMore = result.ids.isNotEmpty(),
            )
        }.toDataFetcher()
    }

    override fun importedMailCategoryFilters(
        user: QlUser,
        query: QlImportedMailCategoryFiltersQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFiltersConnection?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val dataLoader = context.dataLoaders.importedMailCategoryFilterDataLoader.get(env)

        return CompletableFuture.allOf().thenApplyAsync {
            val filterRepository = context.repositoryFactory.createMailFilterRepository()
            val result = filterRepository.getFilters(
                isAsc = query.isAsc,
                userId = userId,
                cursor = query.cursor?.let {
                    ImportedMailCategoryFiltersCursor.fromString(it)
                },
            ).onFailure {
                it.printStackTrace()
            }.getOrNull() ?: return@thenApplyAsync null

            result.items.forEach { item ->
                dataLoader.prime(
                    ImportedMailCategoryFilterDataLoaderDefine.Key(
                        userId = userId,
                        categoryFilterId = item.importedMailCategoryFilterId,
                    ),
                    item,
                )
            }

            QlImportedMailCategoryFiltersConnection(
                nodes = result.items.map {
                    QlImportedMailCategoryFilter(
                        id = it.importedMailCategoryFilterId,
                    )
                },
                cursor = result.cursor?.let { cursor ->
                    ImportedMailCategoryFiltersCursor(cursor).toCursorString()
                },
                isLast = result.items.isEmpty(),
            )
        }.toDataFetcher()
    }

    override fun importedMailCategoryFilter(
        user: QlUser,
        id: ImportedMailCategoryFilterId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilter?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()

        return CompletableFuture.allOf().thenApplyAsync {
            QlImportedMailCategoryFilter(
                id = id,
            )
        }.toDataFetcher()
    }

    override fun moneyUsage(
        user: QlUser,
        id: MoneyUsageId,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsage?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val moneyUsageFuture = context.dataLoaders.moneyUsageDataLoader.get(env)
            .load(
                MoneyUsageDataLoaderDefine.Key(
                    userId = userId,
                    moneyUsageId = id,
                ),
            )
        return CompletableFuture.allOf(moneyUsageFuture).thenApplyAsync {
            QlMoneyUsage(
                id = id,
            )
        }.toDataFetcher()
    }

    override fun moneyUsageAnalytics(
        user: QlUser,
        query: QlMoneyUsageAnalyticsQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageAnalytics>> {
        return CompletableFuture.completedFuture(
            DataFetcherResult.newResult<QlMoneyUsageAnalytics>()
                .data(QlMoneyUsageAnalytics())
                .localContext(
                    MoneyUsageAnalyticsLocalContext(
                        query = query,
                    ),
                )
                .build(),
        )
    }

    override fun moneyUsageAnalyticsByCategory(
        user: QlUser,
        id: MoneyUsageCategoryId,
        query: QlMoneyUsageAnalyticsQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageAnalyticsByCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val results = context.repositoryFactory.createMoneyUsageAnalyticsRepository()
                .getTotalAmountByCategories(
                    userId = userId,
                    sinceDateTimeAt = query.sinceDateTime,
                    untilDateTimeAt = query.untilDateTime,
                )
                .onFailure {
                    it.printStackTrace()
                }.getOrNull() ?: return@supplyAsync DataFetcherResultBuilder.buildNullValue()

            val result = results.firstOrNull { it.categoryId == id }

            DataFetcherResult.newResult<QlMoneyUsageAnalyticsByCategory>()
                .data(
                    QlMoneyUsageAnalyticsByCategory(
                        category = QlMoneyUsageCategory(id),
                        totalAmount = result?.totalAmount ?: 0,
                    ),
                )
                .localContext(
                    MoneyUsageAnalyticsByCategoryLocalContext(
                        query = query,
                    ),
                )
                .build()
        }
    }
}
