package net.matsudamper.money.frontend.feature.admin.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.apolloStore
import com.apollographql.cache.normalized.fetchPolicy
import com.apollographql.cache.normalized.watch
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.AdminUnlinkedImagesTotalCountQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UpdateOperationResponseResult

private const val TAG = "AdminUnlinkedImagesTotalCountModel"

internal class AdminUnlinkedImagesTotalCountModel(
    private val graphqlClient: GraphqlClient,
) {
    private val totalCountQuery = AdminUnlinkedImagesTotalCountQuery()

    fun getFlow(): Flow<ApolloResponse<AdminUnlinkedImagesTotalCountQuery.Data>> {
        return graphqlClient.apolloClient.query(totalCountQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .watch()
            .catch {
                Logger.e(TAG, it)
            }
    }

    internal suspend fun refresh(): UpdateOperationResponseResult<AdminUnlinkedImagesTotalCountQuery.Data> {
        return runCatching {
            val response = graphqlClient.apolloClient.query(totalCountQuery)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
            val data = response.data
                ?: return UpdateOperationResponseResult.Error(
                    NullPointerException("ApolloResponse.data is null"),
                )
            graphqlClient.apolloClient.apolloStore.writeOperation(
                operation = totalCountQuery,
                data = data,
                publish = true,
            )
            UpdateOperationResponseResult.Success(response)
        }.getOrElse { UpdateOperationResponseResult.Error(it) }
    }

    internal suspend fun removeDeletedImagesFromCache(imageIds: Collection<ImageId>) {
        if (imageIds.isEmpty()) return

        graphqlClient.apolloClient.query(totalCountQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .execute()
            .data
            ?.let { before ->
                val connection = before.adminUnlinkedImages
                val nextTotalCount = (connection.totalCount - imageIds.toSet().size).coerceAtLeast(0)
                if (nextTotalCount != connection.totalCount) {
                    graphqlClient.apolloClient.apolloStore.writeOperation(
                        operation = totalCountQuery,
                        data = before.copy(
                            adminUnlinkedImages = connection.copy(
                                totalCount = nextTotalCount,
                            ),
                        ),
                        publish = true,
                    )
                }
            }
    }
}
