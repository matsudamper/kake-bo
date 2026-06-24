package net.matsudamper.money.frontend.feature.admin.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.graphql.AdminUnlinkedImagesQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UpdateOperationResponseResult
import net.matsudamper.money.frontend.graphql.updateOperation

private const val TAG = "AdminUnlinkedImagesPagingModel"

internal class AdminUnlinkedImagesPagingModel(
    private val graphqlClient: GraphqlClient,
) {
    private val firstQuery = AdminUnlinkedImagesQuery(
        size = PAGE_SIZE,
        cursor = Optional.present(null),
    )

    fun getFlow(): Flow<ApolloResponse<AdminUnlinkedImagesQuery.Data>> {
        return graphqlClient.apolloClient.query(firstQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .watch()
            .catch {
                Logger.e(TAG, it)
            }
    }

    internal suspend fun refresh(): UpdateOperationResponseResult<AdminUnlinkedImagesQuery.Data> {
        return graphqlClient.apolloClient.updateOperation(firstQuery) update@{
            success(fetchPage(cursor = null))
        }
    }

    internal suspend fun fetch(): UpdateOperationResponseResult<AdminUnlinkedImagesQuery.Data> {
        return graphqlClient.apolloClient.updateOperation(firstQuery) update@{ before ->
            if (before == null) return@update success(fetchPage(cursor = null))

            val beforeConnection = before.adminUnlinkedImages
            if (beforeConnection.hasMore.not()) return@update noHasMore()

            val cursor = beforeConnection.cursor ?: return@update error()
            val response = fetchPage(cursor = cursor)
            val connection = response.data?.adminUnlinkedImages ?: return@update error()
            success(
                response.newBuilder()
                    .data(
                        before.copy(
                            adminUnlinkedImages = beforeConnection.copy(
                                nodes = beforeConnection.nodes + connection.nodes,
                                cursor = connection.cursor,
                                hasMore = connection.hasMore,
                            ),
                        ),
                    )
                    .build(),
            )
        }
    }

    internal suspend fun removeDeletedImagesFromCache(imageIds: Collection<ImageId>) {
        if (imageIds.isEmpty()) return

        val apolloClient = graphqlClient.apolloClient
        val deletedImageIds = imageIds.toSet()

        apolloClient.query(firstQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .execute()
            .data
            ?.let { before ->
                val connection = before.adminUnlinkedImages
                val filteredNodes = connection.nodes.filterNot { node ->
                    node.id in deletedImageIds
                }
                if (filteredNodes.size != connection.nodes.size) {
                    apolloClient.apolloStore.writeOperation(
                        operation = firstQuery,
                        operationData = before.copy(
                            adminUnlinkedImages = connection.copy(
                                nodes = filteredNodes,
                            ),
                        ),
                        customScalarAdapters = apolloClient.customScalarAdapters,
                        publish = true,
                    )
                }
            }
    }

    private suspend fun fetchPage(
        cursor: String?,
    ): ApolloResponse<AdminUnlinkedImagesQuery.Data> {
        return graphqlClient.apolloClient.query(
            firstQuery.copy(
                cursor = Optional.present(cursor),
            ),
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }

    private companion object {
        private const val PAGE_SIZE = 60
    }
}
