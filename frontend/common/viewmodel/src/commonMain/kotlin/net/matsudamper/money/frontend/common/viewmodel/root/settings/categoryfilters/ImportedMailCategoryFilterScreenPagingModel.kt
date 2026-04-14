package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.watch
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

    internal fun getFlow(): Flow<ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data>> {
        return graphqlClient.apolloClient.query(firstQuery)
            .fetchPolicy(FetchPolicy.CacheFirst)
            .watch()
    }

    internal suspend fun refresh(): UpdateOperationResponseResult<ImportedMailCategoryFiltersScreenPagingQuery.Data> {
        val response = fetch(cursor = null)
        val data = response.data
            ?: return UpdateOperationResponseResult.Error(NullPointerException("ApolloResponse.data is null"))
        graphqlClient.apolloClient.apolloStore.writeOperation(
            operation = firstQuery,
            operationData = data,
            customScalarAdapters = graphqlClient.apolloClient.customScalarAdapters,
            publish = true,
        )
        return UpdateOperationResponseResult.Success(response)
    }

    private val firstQuery = ImportedMailCategoryFiltersScreenPagingQuery(
        query = ImportedMailCategoryFiltersQuery(
            cursor = Optional.present(null),
            isAsc = true,
            size = 10,
            sortType = Optional.present(ImportedMailCategoryFiltersSortType.TITLE),
        ),
    )

    internal suspend fun fetch(): UpdateOperationResponseResult<ImportedMailCategoryFiltersScreenPagingQuery.Data> {
        return graphqlClient.apolloClient.updateOperation(firstQuery) update@{ before ->
            if (before == null) return@update success(fetch(cursor = null))
            if (before.user?.importedMailCategoryFilters?.isLast == true) return@update noHasMore()

            val cursor = before.user?.importedMailCategoryFilters?.cursor ?: return@update error()
            val newData = fetch(cursor = cursor)
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

    private suspend fun fetch(cursor: String?): ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data> {
        return graphqlClient.apolloClient.query(
            query = ImportedMailCategoryFiltersScreenPagingQuery(
                query = ImportedMailCategoryFiltersQuery(
                    cursor = Optional.present(cursor),
                    isAsc = true,
                    size = 10,
                    sortType = Optional.present(ImportedMailCategoryFiltersSortType.TITLE),
                ),
            ),
        ).execute()
    }
}
