package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.graphql.type.MailQuery

class MailImportScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    suspend fun getMail(): ApolloResponse<GetMailQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    GetMailQuery(
                        MailQuery(
                            cursor = Optional.present(null),
                            size = 10,
                        ),
                    ),
                )
                .execute()
        }.getOrNull()
    }
}
