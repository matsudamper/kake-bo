package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.ImageResolver
import net.matsudamper.money.graphql.model.QlImage

class ImageResolverImpl : ImageResolver {
    override fun url(
        image: QlImage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context: GraphQlContext = env.graphQlContext.get(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val domain = ServerEnv.domain
            ?: throw IllegalStateException("DOMAIN is not configured")
        return otelSupplyAsync {
            val userImageRepository = context.diContainer.createUserImageRepository()

            val displayIdMap = userImageRepository.getDisplayIdsByImageIds(
                userId = userId,
                imageIds = listOf(image.id),
            )
            val displayId = displayIdMap[image.id]
                ?: throw IllegalStateException("displayId is not found: imageId=${image.id.value}")

            val imageData = userImageRepository.getImageDataByDisplayId(
                userId = userId,
                displayId = displayId,
            ) ?: throw IllegalStateException("ImageData is not found: displayId=$displayId")

            val gateway = context.diContainer.createReadImageStorageGateway(imageData.storageType)
            gateway.buildDisplayUrl(
                ImageStorageGateway.BuildUrlRequest(
                    domain = domain,
                    displayId = displayId,
                    userId = userId,
                    relativePath = imageData.relativePath,
                    purpose = ImageStorageGateway.Purpose.USER,
                ),
            )
        }.toDataFetcher()
    }
}
