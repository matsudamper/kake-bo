package net.matsudamper.money.backend.graphql.resolver.setting

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.graphql.model.ApiTokenAttributesResolver
import net.matsudamper.money.graphql.model.QlApiToken
import net.matsudamper.money.graphql.model.QlApiTokenAttributes

class ApiTokenAttributesResolverImpl : ApiTokenAttributesResolver {
    override fun apiTokens(
        apiTokenAttributes: QlApiTokenAttributes,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlApiToken>>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        TODO("Not yet implemented")
    }
}
