package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import net.matsudamper.money.frontend.common.base.lib.getNestedMessage
import net.matsudamper.money.frontend.graphql.type.ImportedMailQuery
import net.matsudamper.money.frontend.graphql.type.ImportedMailQueryFilter
import net.matsudamper.money.frontend.graphql.type.ImportedMailSortKey

class MailLinkScreenGraphqlApi(
    private val graphqlClient: GraphqlClient,
) {
    suspend fun getMail(
        cursor: String?,
        isLinked: Boolean?,
    ): ApolloResponse<ImportedMailListScreenMailPagingQuery.Data>? {
        return runCatching {
            graphqlClient.apolloClient
                .query(
                    ImportedMailListScreenMailPagingQuery(
                        query = ImportedMailQuery(
                            cursor = Optional.present(cursor),
                            filter = ImportedMailQueryFilter(
                                isLinked = Optional.present(isLinked),
                            ),
                            sortedBy = ImportedMailSortKey.DATETIME,
                            isAsc = false,
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
