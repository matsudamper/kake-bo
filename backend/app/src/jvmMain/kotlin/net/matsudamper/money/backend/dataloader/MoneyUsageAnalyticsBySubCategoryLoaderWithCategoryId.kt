package net.matsudamper.money.backend.dataloader

import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.matsudamper.money.backend.app.interfaces.MoneyUsageAnalyticsRepository
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.graphql.UserSessionManagerImpl
import net.matsudamper.money.element.MoneyUsageCategoryId
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

internal class MoneyUsageAnalyticsBySubCategoryLoaderWithCategoryId(
    private val repositoryFactory: DiContainer,
    private val userSessionManager: UserSessionManagerImpl,
) : DataLoaderDefine<MoneyUsageAnalyticsBySubCategoryLoaderWithCategoryId.Key, MoneyUsageAnalyticsRepository.TotalAmountBySubCategory> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, MoneyUsageAnalyticsRepository.TotalAmountBySubCategory> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                runBlocking {
                    val repository = repositoryFactory.createMoneyUsageAnalyticsRepository()
                    val userId = userSessionManager.verifyUserSession()

                    val results = keys.groupBy {
                        GroupKey(
                            sinceDateTimeAt = it.sinceDateTimeAt,
                            untilDateTimeAt = it.untilDateTimeAt,
                        )
                    }.map { (key, value) ->
                        val ids = value.map { it.id }

                        async {
                            key to
                                repository.getTotalAmountBySubCategoriesWithCategoryId(
                                    userId = userId,
                                    sinceDateTimeAt = key.sinceDateTimeAt,
                                    untilDateTimeAt = key.untilDateTimeAt,
                                    categoryIds = ids,
                                )
                        }
                    }.map {
                        val (key, kotlinResult) = it.await()
                        kotlinResult
                            .onFailure { e ->
                                TraceLogger.impl().noticeThrowable(e, mapOf(), true)
                            }
                            .getOrNull().orEmpty().map { result ->
                                Key(
                                    id = result.categoryId,
                                    sinceDateTimeAt = key.sinceDateTimeAt,
                                    untilDateTimeAt = key.untilDateTimeAt,
                                ) to result
                            }
                    }.flatten()
                    results.toMap()
                }
            }
        }
    }

    data class Key(
        val id: MoneyUsageCategoryId,
        val sinceDateTimeAt: LocalDateTime,
        val untilDateTimeAt: LocalDateTime,
    )

    private data class GroupKey(
        val sinceDateTimeAt: LocalDateTime,
        val untilDateTimeAt: LocalDateTime,
    )
}
