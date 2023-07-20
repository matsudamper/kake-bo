package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.QlUser
import net.matsudamper.money.graphql.model.QlUserMailAttributes
import net.matsudamper.money.graphql.model.QueryResolver

class QueryResolverImpl : QueryResolver {
    override fun user(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUser?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()
        return CompletableFuture.completedFuture(
            QlUser(
                isLoggedIn = true,
                userMailAttributes = QlUserMailAttributes()
            ),
        ).toDataFetcher()
    }
}