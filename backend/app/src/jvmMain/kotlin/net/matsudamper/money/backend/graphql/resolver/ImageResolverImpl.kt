package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.UserImageRepository
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.feature.image.ImageApiPath
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
        return resolveSummary(image, env) { domain, summary ->
            ImageApiPath.imageV1AbsoluteByDisplayId(
                domain = domain,
                displayId = summary.displayId,
            )
        }
    }

    override fun thumbnailUrl(
        image: QlImage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String?>> {
        return resolveSummary(image, env) { domain, summary ->
            if (summary.hasThumbnail) {
                ImageApiPath.thumbnailV1AbsoluteByDisplayId(
                    domain = domain,
                    displayId = summary.displayId,
                )
            } else {
                null
            }
        }
    }

    private fun <T> resolveSummary(
        image: QlImage,
        env: DataFetchingEnvironment,
        transform: (domain: String, summary: UserImageRepository.ImageSummary) -> T,
    ): CompletionStage<DataFetcherResult<T>> {
        val context: GraphQlContext = env.graphQlContext.get(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val domain = ServerEnv.domain
            ?: throw IllegalStateException("DOMAIN is not configured")
        return otelSupplyAsync {
            val summaryMap = context.diContainer.createUserImageRepository().getImageSummariesByImageIds(
                userId = userId,
                imageIds = listOf(image.id),
            )
            val summary = summaryMap[image.id]
                ?: throw IllegalStateException("image is not found: imageId=${image.id.value}")
            transform(domain, summary)
        }.toDataFetcher()
    }
}
