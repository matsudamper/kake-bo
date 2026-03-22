package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.UserId

class ImportedMailCategoryFiltersDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<UserId, List<MailFilterRepository.MailFilter>> {
    override val key: String = this::class.java.name

    override fun load(keys: Set<UserId>): Map<UserId, List<MailFilterRepository.MailFilter>> {
        val repository = repositoryFactory.createMailFilterRepository()

        return keys.associateWith { userId ->
            repository.getFilters(
                userId = userId,
            )
        }
    }
}
