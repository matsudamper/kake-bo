package net.matsudamper.money.backend.graphql.resolver

import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.QlSession
import net.matsudamper.money.graphql.model.QlSessionAttributes
import net.matsudamper.money.graphql.model.SessionAttributesResolver

class SessionAttributesResolverImpl : SessionAttributesResolver {
    override fun currentSession(sessionAttributes: QlSessionAttributes, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlSession>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val sessionInfo = context.verifyUserSessionAndGetSessionInfo()

        return CompletableFuture.supplyAsync {
            QlSession(
                name = sessionInfo.sessionName,
                lastAccess = sessionInfo.latestAccess.atOffset(ZoneOffset.UTC),
            )
        }.toDataFetcher()
    }

    override fun sessions(sessionAttributes: QlSessionAttributes, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<List<QlSession>>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val sessionInfo = context.verifyUserSessionAndGetSessionInfo()
        val userSessionRepository = context.diContainer.createUserSessionRepository()

        return CompletableFuture.supplyAsync {
            userSessionRepository.getSessions(sessionInfo.userId).map { session ->
                QlSession(
                    name = session.name,
                    lastAccess = session.latestAccess.atOffset(ZoneOffset.UTC),
                )
            }
        }.toDataFetcher()
    }
}
