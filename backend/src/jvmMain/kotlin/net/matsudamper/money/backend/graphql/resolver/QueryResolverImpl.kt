package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.repository.UserSessionRepository
import net.matsudamper.money.graphql.model.QlImportedMailAttributes
import net.matsudamper.money.graphql.model.QlUser
import net.matsudamper.money.graphql.model.QlUserMailAttributes
import net.matsudamper.money.graphql.model.QueryResolver

class QueryResolverImpl : QueryResolver {
    override fun user(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUser?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSession()
        return CompletableFuture.completedFuture(
            QlUser(
                userMailAttributes = QlUserMailAttributes(),
                importedMailAttributes = QlImportedMailAttributes(),
            ),
        ).toDataFetcher()
    }

    override fun isLoggedIn(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val info = context.getSessionInfo()
        return CompletableFuture.supplyAsync {
            when (info) {
                is UserSessionRepository.VerifySessionResult.Failure -> false
                is UserSessionRepository.VerifySessionResult.Success -> true
            }
        }.toDataFetcher()
    }
}
