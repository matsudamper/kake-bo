package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.repository.UserNameRepository
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class UserNameDataLoaderDefine(
    private val userNameRepository: UserNameRepository,
) : DataLoaderDefine<UserId, String> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<UserId, String> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val results = userNameRepository.getUserName(keys.toList())
                keys.associateWith { results[it] }
            }
        }
    }
}
