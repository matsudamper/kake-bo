package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
import net.matsudamper.money.element.ImportedMailCategoryFilterId
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenPagingQuery
import net.matsudamper.money.frontend.graphql.UpdateOperationResponseResult
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersSortType
import net.matsudamper.money.frontend.graphql.updateOperation

public class ImportedMailCategoryFilterScreenPagingModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {

    private val firstQuery = ImportedMailCategoryFiltersScreenPagingQuery(
        query = ImportedMailCategoryFiltersQuery(
            cursor = Optional.present(null),
            isAsc = true,
            size = Optional.present(10),
            sortType = Optional.present(ImportedMailCategoryFiltersSortType.TITLE),
        ),
    )

    internal fun getFlow(): Flow<ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data>> {
        return graphqlClient.apolloClient.query(firstQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .watch()
            .filter { it.data != null }
    }

    internal suspend fun fetch(isForceRefresh: Boolean = false): UpdateOperationResponseResult<ImportedMailCategoryFiltersScreenPagingQuery.Data> {
        return graphqlClient.apolloClient.updateOperation(firstQuery) update@{ before ->
            if (before == null || isForceRefresh) return@update success(fetchPage(cursor = null))
            if (before.user?.importedMailCategoryFilters?.isLast == true) return@update noHasMore()

            val cursor = before.user?.importedMailCategoryFilters?.cursor ?: return@update error()
            val newData = fetchPage(cursor = cursor)
            success(
                newData.newBuilder()
                    .data(
                        data = before.copy(
                            user = before.user?.copy(
                                importedMailCategoryFilters = before.user?.importedMailCategoryFilters?.let { beforeFilters ->
                                    val newFilters = newData.data?.user?.importedMailCategoryFilters
                                        ?: throw IllegalStateException("importedMailCategoryFilters is null")
                                    beforeFilters.copy(
                                        cursor = newFilters.cursor,
                                        isLast = newFilters.isLast,
                                        nodes = beforeFilters.nodes + newFilters.nodes,
                                    )
                                },
                            ),
                        ),
                    )
                    .build(),
            )
        }
    }

    public suspend fun removeFilterFromCache(filterId: ImportedMailCategoryFilterId) {
        graphqlClient.apolloClient.updateOperation(firstQuery) update@{ before ->
            if (before == null) return@update error()
            val connection = before.user?.importedMailCategoryFilters ?: return@update error()
            val filteredNodes = connection.nodes.filterNot { node ->
                node.id == filterId
            }
            if (filteredNodes.size == connection.nodes.size) return@update error()
            val cached = graphqlClient.apolloClient.query(firstQuery)
                .fetchPolicy(FetchPolicy.CacheOnly)
                .execute()
            success(
                cached.newBuilder()
                    .data(
                        data = before.copy(
                            user = before.user?.copy(
                                importedMailCategoryFilters = connection.copy(
                                    nodes = filteredNodes,
                                ),
                            ),
                        ),
                    )
                    .build(),
            )
        }
    }

    private suspend fun fetchPage(cursor: String?): ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data> {
        return withContext(Dispatchers.IO) {
            graphqlClient.apolloClient.query(
                query = ImportedMailCategoryFiltersScreenPagingQuery(
                    query = ImportedMailCategoryFiltersQuery(
                        cursor = Optional.present(cursor),
                        isAsc = true,
                        size = Optional.present(10),
                        sortType = Optional.present(ImportedMailCategoryFiltersSortType.TITLE),
                    ),
                ),
            )
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }
}
