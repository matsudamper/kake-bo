package net.matsudamper.money.frontend.feature.admin.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.AdminSearchUsersQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UpdateOperationResponseResult
import net.matsudamper.money.frontend.graphql.updateOperation

private const val TAG = "AdminUserSearchPagingModel"

internal class AdminUserSearchPagingModel(
    private val graphqlClient: GraphqlClient,
) {
    fun getFlow(firstQuery: AdminSearchUsersQuery): Flow<ApolloResponse<AdminSearchUsersQuery.Data>> {
        return graphqlClient.apolloClient.query(firstQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .watch()
            .catch { Logger.e(TAG, it) }
    }

    public suspend fun refresh(firstQuery: AdminSearchUsersQuery): UpdateOperationResponseResult<AdminSearchUsersQuery.Data> {
        return graphqlClient.apolloClient.updateOperation(firstQuery) update@{
            success(fetchPage(firstQuery = firstQuery, cursor = null))
        }
    }

    public suspend fun fetch(firstQuery: AdminSearchUsersQuery): UpdateOperationResponseResult<AdminSearchUsersQuery.Data> {
        return graphqlClient.apolloClient.updateOperation(firstQuery) update@{ before ->
            if (before == null) return@update success(fetchPage(firstQuery = firstQuery, cursor = null))

            val connection = before.adminSearchUsers
            if (connection.hasMore.not()) return@update noHasMore()

            val cursor = connection.cursor ?: return@update error()
            val response = fetchPage(firstQuery = firstQuery, cursor = cursor)
            val newConnection = response.data?.adminSearchUsers ?: return@update error()

            success(
                response.newBuilder()
                    .data(
                        before.copy(
                            adminSearchUsers = connection.copy(
                                nodes = connection.nodes + newConnection.nodes,
                                cursor = newConnection.cursor,
                                hasMore = newConnection.hasMore,
                            ),
                        ),
                    )
                    .build(),
            )
        }
    }

    private suspend fun fetchPage(
        firstQuery: AdminSearchUsersQuery,
        cursor: String?,
    ): ApolloResponse<AdminSearchUsersQuery.Data> {
        return graphqlClient.apolloClient.query(
            firstQuery.copy(
                cursor = Optional.present(cursor),
            ),
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }
}
