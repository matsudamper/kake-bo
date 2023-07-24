package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.MutationResolver
import net.matsudamper.money.graphql.model.QlAdminMutation
import net.matsudamper.money.graphql.model.QlUserMutation

class MutationResolverImpl : MutationResolver {
    override fun adminMutation(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlAdminMutation>> {
        return CompletableFuture.completedFuture(QlAdminMutation()).toDataFetcher()
    }

    override fun userMutation(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUserMutation>> {
        return CompletableFuture.completedFuture(QlUserMutation()).toDataFetcher()
    }
}
