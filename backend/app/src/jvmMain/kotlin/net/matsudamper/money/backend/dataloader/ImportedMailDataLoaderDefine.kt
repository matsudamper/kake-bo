package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.ImportedMailRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.UserId

class ImportedMailDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<ImportedMailDataLoaderDefine.Key, ImportedMailRepository.Mail> {
    override val key: String = this::class.java.name

    override fun load(keys: Set<Key>): Map<Key, ImportedMailRepository.Mail> {
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

        return keys.associateWith { key ->
            result[key] ?: throw IllegalStateException("not result key: $key")
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
