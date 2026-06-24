package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.element.MailId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.type.MailQuery

private const val TAG = "MailImportScreenGraphqlApi"

class MailImportScreenGraphqlApi(
    private val graphqlClient: GraphqlClient,
) {
    suspend fun getMail(cursor: String?): ApolloResponse<GetMailQuery.Data>? {
        return runCatching {
            graphqlClient.apolloClient
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
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    suspend fun mailImport(mailIds: List<MailId>): ApolloResponse<ImportMailMutation.Data>? {
        return runCatching {
            graphqlClient.apolloClient
                .mutation(
                    ImportMailMutation(
                        mailIds = mailIds,
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    suspend fun deleteMail(mailIds: List<MailId>): ApolloResponse<DeleteMailMutation.Data>? {
        return runCatching {
            graphqlClient.apolloClient
                .mutation(
                    DeleteMailMutation(
                        mailIds = mailIds,
                    ),
                )
                .execute()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }
}
