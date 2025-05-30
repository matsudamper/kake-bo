package net.matsudamper.money.backend.dataloader

import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.lib.flatten
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class MoneyUsageDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<MoneyUsageDataLoaderDefine.Key, MoneyUsageDataLoaderDefine.MoneyUsage> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, MoneyUsage> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val dbMailRepository = repositoryFactory.createMoneyUsageRepository()

                val result = keys.groupBy { it.userId }
                    .map { (userId, key) ->
                        dbMailRepository
                            .getMoneyUsage(
                                userId = userId,
                                ids = key.map { it.moneyUsageId },
                            ).fold(
                                onSuccess = { results ->
                                    results.associate {
                                        Key(
                                            userId = userId,
                                            moneyUsageId = it.id,
                                        ) to
                                            MoneyUsage(
                                                id = it.id,
                                                userId = it.userId,
                                                amount = it.amount,
                                                subCategoryId = it.subCategoryId,
                                                date = it.date,
                                                title = it.title,
                                                description = it.description,
                                            )
                                    }
                                },
                                onFailure = { mapOf() },
                            )
                    }.flatten()

                keys.associateWith { key ->
                    result[key] ?: throw IllegalStateException("not result key: $key")
                }
            }
        }
    }

    data class Key(
        val userId: UserId,
        val moneyUsageId: MoneyUsageId,
    )

    data class MoneyUsage(
        val id: MoneyUsageId,
        val description: String,
        val title: String,
        val date: LocalDateTime,
        val subCategoryId: MoneyUsageSubCategoryId?,
        val amount: Int,
        val userId: UserId,
    )
}
