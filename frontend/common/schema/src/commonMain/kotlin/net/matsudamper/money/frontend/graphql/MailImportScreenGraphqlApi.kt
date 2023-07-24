package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.element.MailId
import net.matsudamper.money.frontend.graphql.type.MailQuery

class MailImportScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    suspend fun getMail(
        cursor: String?,
    ): ApolloResponse<GetMailQuery.Data>? {
        return runCatching {
            apolloClient
                .query(
                    GetMailQuery(
                        MailQuery(
                            cursor = Optional.present(cursor),
                            size = 10,
                        ),
                    ),
                )
                .execute()
        }.getOrNull()
    }

    suspend fun mailImport(
        mailIds: List<MailId>,
    ) {
        runCatching {
            apolloClient
                .mutation(
                    ImportMailMutation(
                        mailIds = mailIds,
                    ),
                )
                .execute()
        }.getOrNull()
    }
}
