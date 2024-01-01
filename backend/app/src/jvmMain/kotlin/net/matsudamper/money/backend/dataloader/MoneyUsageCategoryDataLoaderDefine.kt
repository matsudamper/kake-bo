package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.datasource.db.repository.MoneyUsageCategoryRepository
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.lib.flatten
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class MoneyUsageCategoryDataLoaderDefine(
    private val repositoryFactory: RepositoryFactory,
) : DataLoaderDefine<MoneyUsageCategoryDataLoaderDefine.Key, MoneyUsageCategoryDataLoaderDefine.Result> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, Result> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val repository = repositoryFactory.createMoneyUsageCategoryRepository()

                val results = keys.groupBy { it.userId }
                    .mapNotNull { (userId, key) ->
                        val result = repository
                            .getCategory(
                                userId = userId,
                                moneyUsageCategoryIds = key.map { it.categoryId },
                            )

                        when (result) {
                            is MoneyUsageCategoryRepository.GetCategoryResult.Failed -> {
                                result.e.printStackTrace()
                                null
                            }
                            is MoneyUsageCategoryRepository.GetCategoryResult.Success -> {
                                result.results.associateBy {
                                    Key(
                                        userId = userId,
                                        categoryId = it.moneyUsageCategoryId,
                                    )
                                }
                            }
                        }
                    }.flatten()

                keys.associateWith { key ->
                    val result = results[key] ?: return@associateWith null
                    Result(
                        userId = key.userId,
                        name = result.name,
                        categoryId = result.moneyUsageCategoryId,
                    )
                }
            }
        }
    }

    data class Result(
        val userId: UserId,
        val name: String,
        val categoryId: MoneyUsageCategoryId,
    )

    data class Key(
        val userId: UserId,
        val categoryId: MoneyUsageCategoryId,
    )
}
