package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.AdminUnlinkedImagesConnectionResolver
import net.matsudamper.money.graphql.model.QlAdminUnlinkedImagesConnection

class AdminUnlinkedImagesConnectionResolverImpl : AdminUnlinkedImagesConnectionResolver {
    override fun totalCount(
        adminUnlinkedImagesConnection: QlAdminUnlinkedImagesConnection,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyAdminSession()
        return otelSupplyAsync {
            context.diContainer.createAdminImageRepository().countUnlinkedImages()
        }.toDataFetcher()
    }
}
