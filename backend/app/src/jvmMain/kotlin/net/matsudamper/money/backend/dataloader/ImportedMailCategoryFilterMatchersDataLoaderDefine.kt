package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.element.UserId
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class ImportedMailCategoryFilterMatchersDataLoaderDefine(
    private val repositoryFactory: DiContainer,
) : DataLoaderDefine<UserId, List<MailFilterRepository.Matcher>> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<UserId, List<MailFilterRepository.Matcher>> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            otelSupplyAsync {
                val repository = repositoryFactory.createMailFilterRepository()
                keys.associateWith { userId ->
                    repository.getMatchers(
                        userId = userId,
                    )
                }
            }
        }
    }
}
