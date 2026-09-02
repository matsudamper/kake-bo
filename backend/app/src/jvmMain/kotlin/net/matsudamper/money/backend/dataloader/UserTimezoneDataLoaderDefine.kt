package net.matsudamper.money.backend.dataloader

import java.time.ZoneOffset
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.element.UserId
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

class UserTimezoneDataLoaderDefine(
    private val diContainer: DiContainer,
) : DataLoaderDefine<UserId, ZoneOffset> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<UserId, ZoneOffset> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            otelSupplyAsync {
                val userConfigRepository = diContainer.createUserConfigRepository()
                keys.associateWith { userId ->
                    userConfigRepository.getTimezoneOffset(userId) ?: ZoneOffset.UTC
                }
            }
        }
    }
}
