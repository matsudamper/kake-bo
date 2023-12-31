package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.repository.MoneyUsageSubCategoryRepository
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.lib.flatten
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class MoneyUsageSubCategoryDataLoaderDefine(
    private val repositoryFactory: RepositoryFactory,
) : DataLoaderDefine<MoneyUsageSubCategoryDataLoaderDefine.Key, MoneyUsageSubCategoryDataLoaderDefine.Result> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, Result> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val repository = repositoryFactory.createMoneyUsageSubCategoryRepository()

                val results = keys.groupBy { it.userId }
                    .mapNotNull { (userId, key) ->
                        val result = repository
                            .getSubCategory(
                                userId = userId,
                                moneyUsageSubCategoryIds = key.map { it.subCategoryId },
                            )

                        when (result) {
                            is MoneyUsageSubCategoryRepository.GetSubCategoryResult.Failed -> {
                                result.e.printStackTrace()
                                null
                            }
                            is MoneyUsageSubCategoryRepository.GetSubCategoryResult.Success -> {
                                result.results.associateBy {
                                    Key(
                                        userId = userId,
                                        subCategoryId = it.moneyUsageSubCategoryId,
                                    )
                                }
                            }
                        }
                    }.flatten()

                keys.associateWith { key ->
                    val result = results[key] ?: return@associateWith null
                    Result(
                        userId = key.userId,
                        subCategoryId = key.subCategoryId,
                        name = result.name,
                        categoryId = result.moneyUsageCategoryId,
                    )
                }
            }
        }
    }

    data class Result(
        val userId: UserId,
        val subCategoryId: MoneyUsageSubCategoryId,
        val name: String,
        val categoryId: MoneyUsageCategoryId,
    )

    data class Key(
        val userId: UserId,
        val subCategoryId: MoneyUsageSubCategoryId,
    )
}
