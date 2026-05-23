package net.matsudamper.money.backend.dataloader

import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.feature.session.UserSessionManagerImpl
import net.matsudamper.money.backend.graphql.GraphqlMoneyException
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.element.ImageId
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

internal class UserImageUrlDataLoaderDefine(
    private val diContainer: DiContainer,
    private val userSessionManager: UserSessionManagerImpl,
) : DataLoaderDefine<ImageId, String> {
    override val key: String = this::class.java.name

    override fun getDataLoader(): DataLoader<ImageId, String> {
        return DataLoaderFactory.newMappedDataLoader { keys, _ ->
            otelSupplyAsync {
                val userId = userSessionManager.verifyUserSession()
                    ?: throw GraphqlMoneyException.SessionNotVerify()
                val domain = ServerEnv.domain
                    ?: throw IllegalStateException("DOMAIN is not configured")

                val imageInfoMap = diContainer.createUserImageRepository()
                    .getImageInfoByImageIds(userId = userId, imageIds = keys.toList())

                val requestsByStorage = imageInfoMap.entries.groupBy { it.value.storageType }
                val urlByDisplayId = buildMap<String, String> {
                    for ((storageType, entries) in requestsByStorage) {
                        val gateway = diContainer.createReadImageStorageGateway(storageType)
                        val requests = entries.map { (_, info) ->
                            ImageStorageGateway.BuildUrlRequest(
                                domain = domain,
                                displayId = info.displayId,
                                userId = userId,
                                relativePath = info.relativePath,
                                purpose = ImageStorageGateway.Purpose.USER,
                            )
                        }
                        putAll(gateway.buildDisplayUrls(requests))
                    }
                }

                keys.associateWith { imageId ->
                    val info = imageInfoMap[imageId]
                        ?: throw IllegalStateException("displayId is not found: imageId=${imageId.value}")
                    urlByDisplayId[info.displayId]
                        ?: throw IllegalStateException("url is not found: imageId=${imageId.value}")
                }
            }
        }
    }
}
