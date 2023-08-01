package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.repository.MoneyUsageSubCategoryRepository
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.lib.flatten
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class MoneyUsageAssociateByImportedMailDataLoaderDefine(
    private val repositoryFactory: RepositoryFactory,
) : DataLoaderDefine<MoneyUsageAssociateByImportedMailDataLoaderDefine.Key, MoneyUsageAssociateByImportedMailDataLoaderDefine.Result> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, Result> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val repository = repositoryFactory.createDbMailRepository()

                val results = keys.groupBy { it.userId }
                    .mapNotNull { (userId, key) ->
                        val result = repository
                            .getMails(
                                userId = userId,
                                moneyUsageIdList = key.map { it.moneyUsageId },
                            ).onFailure {
                                it.printStackTrace()
                            }.getOrNull() ?: return@supplyAsync null

                        result.map { (usageId, mailId) ->
                            Key(
                                userId = userId,
                                moneyUsageId = usageId,
                            ) to mailId
                        }.toMap()
                    }.flatten()


                keys.associateWith { key ->
                    val result = results[key] ?: return@associateWith null
                    Result(
                        userId = key.userId,
                        mailIdList = result,
                    )
                }
            }
        }
    }

    data class Result(
        val userId: UserId,
        val mailIdList: List<ImportedMailId>,
    )

    data class Key(
        val userId: UserId,
        val moneyUsageId: MoneyUsageId,
    )
}
