package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.UserId

class ImportedMailCategoryFilterConditionsDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<UserId, List<MailFilterRepository.Condition>> {
    override val key: String = this::class.java.name

    override fun load(keys: Set<UserId>): Map<UserId, List<MailFilterRepository.Condition>> {
        val repository = repositoryFactory.createMailFilterRepository()
        return keys.associateWith { userId ->
            repository.getConditions(
                userId = userId,
            )
        }
    }
}
