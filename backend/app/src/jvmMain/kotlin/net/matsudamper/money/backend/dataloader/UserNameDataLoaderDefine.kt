package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import net.matsudamper.money.backend.app.interfaces.UserRepository
import net.matsudamper.money.element.UserId
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class UserNameDataLoaderDefine(
    private val userRepository: UserRepository,
) : DataLoaderDefine<UserId, String> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<UserId, String> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            CompletableFuture.supplyAsync {
                val results = userRepository.getUserName(keys.toList())
                keys.associateWith { results[it] }
            }
        }
    }
}
