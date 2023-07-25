package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.repository.DbMailRepository
import net.matsudamper.money.element.ImportedMailId
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class ImportedMailDataLoaderDefine(
    private val repositoryFactory: RepositoryFactory,
) : DataLoaderDefine<ImportedMailDataLoaderDefine.Key, DbMailRepository.Mail> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, DbMailRepository.Mail> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val dbMailRepository = repositoryFactory.createDbMailRepository()

                val result = keys.groupBy { it.userId }
                    .map { (userId, key) ->
                        dbMailRepository
                            .getMails(
                                userId = userId,
                                mailIds = key.map { it.importedMailId },
                            ).associateBy {
                                Key(
                                    userId = userId,
                                    importedMailId = it.id,
                                )
                            }
                    }.flatten()

                keys.associateWith { key ->
                    result[key]
                }
            }
        }
    }

    private fun <K, V> List<Map<K, V>>.flatten(): Map<K, V> {
        return this.fold(emptyMap()) { result, item ->
            result + item
        }
    }

    data class Key(
        val userId: UserId,
        val importedMailId: ImportedMailId,
    )
}
