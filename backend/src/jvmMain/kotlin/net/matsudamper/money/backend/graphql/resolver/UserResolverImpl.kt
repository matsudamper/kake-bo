package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.dataloader.ImportedMailCategoryFilterDataLoaderDefine
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.lib.CursorParser
import net.matsudamper.money.backend.repository.MailFilterRepository
import net.matsudamper.money.backend.repository.MoneyUsageCategoryRepository
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFilter
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFiltersConnection
import net.matsudamper.money.graphql.model.QlImportedMailCategoryFiltersQuery
import net.matsudamper.money.graphql.model.QlMoneyUsage
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
                    isAsc = false,
                    lastId = query.cursor?.let { MoneyUsagesCursor.fromString(it) }?.lastId,
                )
            val resultIds = results.getOrThrow()
            QlMoneyUsagesConnection(
                nodes = resultIds.map { id ->
                    QlMoneyUsage(
                        id = id,
                    )
                },
                cursor = resultIds.lastOrNull()?.let { MoneyUsagesCursor(it).toCursorString() },
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

    override fun importedMailCategoryFilter(user: QlUser, id: ImportedMailCategoryFilterId, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlImportedMailCategoryFilter?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()

        return CompletableFuture.allOf().thenApplyAsync {
            QlImportedMailCategoryFilter(
                id = id,
            )
        }.toDataFetcher()
    }
}

private class MoneyUsagesCursor(
    val lastId: MoneyUsageId,
) {
    fun toCursorString(): String {
        return CursorParser.createToString(
            mapOf(
                LAST_ID_KEY to lastId.id.toString(),
            ),
        )
    }

    companion object {
        private const val LAST_ID_KEY = "lastId"
        fun fromString(cursorString: String): MoneyUsagesCursor {
            return MoneyUsagesCursor(
                lastId = MoneyUsageId(
                    CursorParser.parseFromString(cursorString)[LAST_ID_KEY]!!.toInt(),
                ),
            )
        }
    }
}

private class ImportedMailCategoryFiltersCursor(
    private val cursor: MailFilterRepository.MailFilterCursor,
) {
    fun toCursorString(): String {
        return CursorParser.createToString(
            mapOf(
                ID to cursor.id.id.toString(),
                ORDER_NUM to cursor.orderNumber.toString(),
            ),
        )
    }

    companion object {
        private const val ID = "ID"
        private const val ORDER_NUM = "ORDER_NUM"
        fun fromString(cursorString: String): MailFilterRepository.MailFilterCursor {
            val map = CursorParser.parseFromString(cursorString)
            return MailFilterRepository.MailFilterCursor(
                id = ImportedMailCategoryFilterId(map[ID]!!.toInt()),
                orderNumber = map[ORDER_NUM]!!.toInt(),
            )
        }
    }
}
