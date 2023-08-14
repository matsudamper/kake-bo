package net.matsudamper.money.backend.dataloader

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.graphql.UserIdVerifyUseCase
import net.matsudamper.money.backend.repository.MoneyUsageAnalyticsRepository
import net.matsudamper.money.element.MoneyUsageCategoryId
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class MoneyUsageAnalyticsBySubCategoryLoader(
    private val repositoryFactory: RepositoryFactory,
    private val userIdVerifyUseCase: UserIdVerifyUseCase,
) : DataLoaderDefine<MoneyUsageAnalyticsBySubCategoryLoader.Key, MoneyUsageAnalyticsRepository.TotalAmountBySubCategory> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, MoneyUsageAnalyticsRepository.TotalAmountBySubCategory> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                runBlocking {
                    val repository = repositoryFactory.createMoneyUsageAnalyticsRepository()
                    val userId = userIdVerifyUseCase.verifyUserSession()

                    val results = keys.groupBy {
                        GroupKey(
                            sinceDateTimeAt = it.sinceDateTimeAt,
                            untilDateTimeAt = it.untilDateTimeAt,
                        )
                    }.map { (key, value) ->
                        val ids = value.map { it.id }

                        async {
                            key to repository.getTotalAmountBySubCategories(
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
                                e.printStackTrace()
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
