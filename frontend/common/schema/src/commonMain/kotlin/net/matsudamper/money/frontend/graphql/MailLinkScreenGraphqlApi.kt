package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.common.base.lib.getNestedMessage
import net.matsudamper.money.frontend.graphql.type.ImportedMailQuery

class MailLinkScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    suspend fun getMail(
        cursor: String?,
    ): ApolloResponse<MailLinkScreenGetMailsQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    MailLinkScreenGetMailsQuery(
                        query = ImportedMailQuery(
                            cursor = Optional.present(cursor),
                            size = 10,
                        ),
                    ),
                )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.onFailure {
            println("error: ${it.getNestedMessage()}")
        }.getOrNull()
    }
}
