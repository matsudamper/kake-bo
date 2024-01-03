package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
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
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }.getOrNull()
    }

    suspend fun mailImport(
        mailIds: List<MailId>,
    ): ApolloResponse<ImportMailMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    ImportMailMutation(
                        mailIds = mailIds,
                    ),
                )
                .execute()
        }.getOrNull()
    }
    suspend fun deleteMail(
        mailIds: List<MailId>,
    ): ApolloResponse<DeleteMailMutation.Data>? {
        return runCatching {
            apolloClient
                .mutation(
                    DeleteMailMutation(
                        mailIds = mailIds,
                    ),
                )
                .execute()
        }.getOrNull()
    }
}
