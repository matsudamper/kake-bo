package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelThenApplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.ImageResolver
import net.matsudamper.money.graphql.model.QlImage

class ImageResolverImpl : ImageResolver {
    override fun url(
        image: QlImage,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<String>> {
        val context: GraphQlContext = env.graphQlContext.get(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()
        val future = context.dataLoaders.userImageUrlDataLoader.get(env).load(image.id)
        return CompletableFuture.allOf(future).otelThenApplyAsync {
            future.get()!!
        }.toDataFetcher()
    }
}
