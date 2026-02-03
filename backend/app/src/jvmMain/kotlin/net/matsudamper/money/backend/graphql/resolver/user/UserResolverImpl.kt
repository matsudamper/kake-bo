package net.matsudamper.money.backend.graphql.resolver.user

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.app.interfaces.MoneyUsageRepository
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.dataloader.MoneyUsageAnalyticsBySubCategoryLoaderWithSubCategoryId
import net.matsudamper.money.backend.dataloader.MoneyUsageDataLoaderDefine
import net.matsudamper.money.backend.graphql.DataFetcherResultBuilder
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageAnalyticsByCategoryLocalContext
import net.matsudamper.money.backend.graphql.localcontext.MoneyUsageAnalyticsLocalContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.graphql.model.QlApiTokenAttributes
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilter
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFiltersConnection
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFiltersQuery
import net.matsudamper.money.graphql.model.QlMoneyUsage
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalytics
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsByCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsBySubCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageAnalyticsQuery
import net.matsudamper.money.graphql.model.QlMoneyUsageCategoriesConnection
import net.matsudamper.money.graphql.model.QlMoneyUsageCategoriesInput
import net.matsudamper.money.graphql.model.QlMoneyUsageCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategory
import net.matsudamper.money.graphql.model.QlMoneyUsageSubCategoryInput
import net.matsudamper.money.graphql.model.QlMoneyUsagesConnection
import net.matsudamper.money.graphql.model.QlMoneyUsagesQuery
import net.matsudamper.money.graphql.model.QlMoneyUsagesQueryOrderType
import net.matsudamper.money.graphql.model.QlSessionAttributes
import net.matsudamper.money.graphql.model.QlUser
import net.matsudamper.money.graphql.model.QlUserSettings
import net.matsudamper.money.graphql.model.UserResolver

class UserResolverImpl : UserResolver {
    override fun settings(
        user: QlUser,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlUserSettings>> {
        return CompletableFuture.completedFuture(
            QlUserSettings(
                sessionAttributes = QlSessionAttributes(),
                apiTokenAttributes = QlApiTokenAttributes(),
            ),
        ).toDataFetcher()
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
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val result = context.diContainer.createMoneyUsageCategoryRepository()
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
                        // TODO
                        cursor = null,
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
        context.verifyUserSessionAndGetUserId()

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
        context.verifyUserSessionAndGetUserId()

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
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val results = context.diContainer.createMoneyUsageRepository()
                .getMoneyUsageByQuery(
                    userId = userId,
                    size = query.size,
                    isAsc = query.isAsc,
                    cursor = query.cursor?.let { MoneyUsagesCursor.fromString(it) }?.let {
                        MoneyUsageRepository.GetMoneyUsageByQueryResult.Cursor(
                            lastId = it.lastId,
                            date = it.lastDate,
                            amount = it.amount,
                        )
                    },
                    sinceDateTime = query.filter?.sinceDateTime,
                    untilDateTime = query.filter?.untilDateTime,
                    categoryIds = query.filter?.category.orEmpty(),
                    subCategoryIds = query.filter?.subCategory.orEmpty(),
                    text = query.filter?.text?.takeIf { it.isNotBlank() },
                    orderType = when (query.orderType) {
                        null,
                        QlMoneyUsagesQueryOrderType.DATE,
                        ->
                            MoneyUsageRepository.OrderType.DATE

                        QlMoneyUsagesQueryOrderType.AMOUNT ->
                            MoneyUsageRepository.OrderType.AMOUNT
                    },
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
                        amount = cursor.amount,
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
        val userId = context.verifyUserSessionAndGetUserId()
        val dataLoader = context.dataLoaders.importedMailCategoryFilterDataLoader.get(env)

        return CompletableFuture.allOf().thenApplyAsync {
            val filterRepository = context.diContainer.createMailFilterRepository()
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
        context.verifyUserSessionAndGetUserId()

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
        val userId = context.verifyUserSessionAndGetUserId()
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
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val results = context.diContainer.createMoneyUsageAnalyticsRepository()
                .getTotalAmountByCategories(
                    userId = userId,
                    sinceDateTimeAt = query.sinceDateTime,
                    untilDateTimeAt = query.untilDateTime,
                )
                .onFailure {
                    it.printStackTrace()
                }.getOrNull() ?: return@supplyAsync DataFetcherResultBuilder.buildNullValue()

            val result = results.firstOrNull { it.categoryId == id }

            DataFetcherResultBuilder.nullable(
                value = QlMoneyUsageAnalyticsByCategory(
                    category = QlMoneyUsageCategory(id),
                    totalAmount = result?.totalAmount ?: 0,
                ),
                localContext = MoneyUsageAnalyticsByCategoryLocalContext(
                    query = query,
                ),
            ).build()
        }
    }

    override fun moneyUsageAnalyticsBySubCategory(
        user: QlUser,
        id: MoneyUsageSubCategoryId,
        query: QlMoneyUsageAnalyticsQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlMoneyUsageAnalyticsBySubCategory?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val future = context.dataLoaders.moneyUsageAnalyticsBySubCategoryLoaderWithSubCategoryId.get(env).load(
            MoneyUsageAnalyticsBySubCategoryLoaderWithSubCategoryId.Key(
                id = id,
                sinceDateTimeAt = query.sinceDateTime,
                untilDateTimeAt = query.untilDateTime,
            ),
        )

        return CompletableFuture.allOf(future).thenApplyAsync {
            val result = future.get() ?: return@thenApplyAsync null

            QlMoneyUsageAnalyticsBySubCategory(
                subCategory = QlMoneyUsageSubCategory(result.id),
                totalAmount = result.totalAmount,
            )
        }.toDataFetcher()
    }
}
