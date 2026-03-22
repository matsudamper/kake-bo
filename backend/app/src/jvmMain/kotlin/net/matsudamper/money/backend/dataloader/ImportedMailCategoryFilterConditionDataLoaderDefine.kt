package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.ImportedMailCategoryFilterConditionId
import net.matsudamper.money.element.UserId
import net.matsudamper.money.lib.flatten

class ImportedMailCategoryFilterConditionDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<ImportedMailCategoryFilterConditionDataLoaderDefine.Key, MailFilterRepository.Condition> {
    override val key: String = this::class.java.name

    override fun load(keys: Set<Key>): Map<Key, MailFilterRepository.Condition> {
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

        return keys.associateWith { key ->
            results[key] ?: throw IllegalStateException("not result key: $key")
        }
    }

    data class Key(
        val userId: UserId,
        val conditionId: ImportedMailCategoryFilterConditionId,
    )
}
