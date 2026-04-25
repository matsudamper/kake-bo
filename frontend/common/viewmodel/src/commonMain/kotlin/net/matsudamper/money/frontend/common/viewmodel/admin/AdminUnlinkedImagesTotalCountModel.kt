package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.AdminUnlinkedImagesTotalCountQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UpdateOperationResponseResult

private const val TAG = "AdminUnlinkedImagesTotalCountModel"

public class AdminUnlinkedImagesTotalCountModel(
    private val graphqlClient: GraphqlClient,
) {
    private val totalCountQuery = AdminUnlinkedImagesTotalCountQuery()

    internal fun getFlow(): Flow<ApolloResponse<AdminUnlinkedImagesTotalCountQuery.Data>> {
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
                operationData = data,
                customScalarAdapters = graphqlClient.apolloClient.customScalarAdapters,
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
                        operationData = before.copy(
                            adminUnlinkedImages = connection.copy(
                                totalCount = nextTotalCount,
                            ),
                        ),
                        customScalarAdapters = graphqlClient.apolloClient.customScalarAdapters,
                        publish = true,
                    )
                }
            }
    }
}
