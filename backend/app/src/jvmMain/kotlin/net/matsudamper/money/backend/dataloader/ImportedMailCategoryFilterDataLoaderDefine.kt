package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.lib.flatten

class ImportedMailCategoryFilterDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<ImportedMailCategoryFilterDataLoaderDefine.Key, MailFilterRepository.MailFilter> {
    override val key: String = this::class.java.name

    override fun load(keys: Set<Key>): Map<Key, MailFilterRepository.MailFilter> {
        val repository = repositoryFactory.createMailFilterRepository()

        val results = keys.groupBy { it.userId }
            .mapNotNull { (userId, keys) ->
                val results = repository.getFilters(
                    userId = userId,
                    categoryFilterIds = keys.map { it.categoryFilterId },
                ).map { result ->
                    result.associateBy { it.importedMailCategoryFilterId }
                }.onFailure {
                    it.printStackTrace()
                }.getOrNull() ?: return@mapNotNull null

                keys.associateWith { key ->
                    results[key.categoryFilterId]
                }
            }.flatten()

        return keys.associateWith { key ->
            results[key] ?: throw IllegalStateException("not result key: $key")
        }
    }

    data class Key(
        val userId: UserId,
        val categoryFilterId: ImportedMailCategoryFilterId,
    )
}
