package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.repository.MailFilterRepository
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.lib.flatten
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class ImportedMailCategoryFilterConditionDataLoaderDefine(
    private val repositoryFactory: RepositoryFactory,
) : DataLoaderDefine<ImportedMailCategoryFilterConditionDataLoaderDefine.Key, MailFilterRepository.Condition> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, MailFilterRepository.Condition> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val repository = repositoryFactory.createMailFilterRepository()

                val results = keys.groupBy { it.userId }
                    .mapNotNull { (userId, keys) ->
                        val results = repository.getConditions(
                            userId = userId,
                            filterIds = keys.map { it.conditionId },
                        ).map {
                            it.associateBy { it.conditionId }
                        }.onFailure {
                            it.printStackTrace()
                        }.getOrNull() ?: return@mapNotNull null

                        keys.associateWith { key ->
                            results[key.conditionId]
                        }
                    }.flatten()

                keys.associateWith { key ->
                    results[key] ?: return@associateWith null
                }
            }
        }
    }

    data class Key(
        val userId: UserId,
        val conditionId: ImportedMailCategoryFilterConditionId,
    )
}
