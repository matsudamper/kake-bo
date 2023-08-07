package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.repository.MailFilterRepository
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class ImportedMailCategoryFiltersDataLoaderDefine(
    private val repositoryFactory: RepositoryFactory,
) : DataLoaderDefine<UserId, List<MailFilterRepository.MailFilter>> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<UserId, List<MailFilterRepository.MailFilter>> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val repository = repositoryFactory.createMailFilterRepository()

                keys.associateWith { userId ->
                    repository.getFilters(
                        userId = userId,
                    )
                }
            }
        }
    }
}
