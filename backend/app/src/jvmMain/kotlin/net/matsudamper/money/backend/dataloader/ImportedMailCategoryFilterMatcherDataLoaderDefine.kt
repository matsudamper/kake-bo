package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.element.ImportedMailCategoryFilterMatcherId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.lib.flatten
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class ImportedMailCategoryFilterMatcherDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<ImportedMailCategoryFilterMatcherDataLoaderDefine.Key, MailFilterRepository.Matcher> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<Key, MailFilterRepository.Matcher> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            otelSupplyAsync {
                val repository = repositoryFactory.createMailFilterRepository()

                val results = keys.groupBy { it.userId }
                    .mapNotNull { (userId, keys) ->
                        val results = repository.getMatchers(
                            userId = userId,
                            matcherIds = keys.map { it.matcherId },
                        ).map {
                            it.associateBy { it.matcherId }
                        }.onFailure {
                            it.printStackTrace()
                        }.getOrNull() ?: return@mapNotNull null

                        keys.associateWith { key ->
                            results[key.matcherId]
                        }
                    }.flatten()

                keys.associateWith { key ->
                    results[key] ?: throw IllegalStateException("not result key: $key")
                }
            }
        }
    }

    data class Key(
        val userId: UserId,
        val matcherId: ImportedMailCategoryFilterMatcherId,
    )
}
