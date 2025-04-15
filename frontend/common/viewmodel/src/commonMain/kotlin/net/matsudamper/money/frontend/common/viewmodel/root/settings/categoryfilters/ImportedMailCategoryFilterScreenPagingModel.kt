package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters

import kotlinx.coroutines.flow.Flow
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenPagingQuery
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery
import net.matsudamper.money.frontend.graphql.updateOperation

public class ImportedMailCategoryFilterScreenPagingModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {

    internal fun getFlow(): Flow<ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data>> {
        return graphqlClient.apolloClient.query(firstQuery)
            .fetchPolicy(FetchPolicy.CacheOnly)
            .watch()
    }

    internal fun clear() {
        graphqlClient.apolloClient.apolloStore.remove(CacheKey(firstQuery.name()))
    }

    private val firstQuery = ImportedMailCategoryFiltersScreenPagingQuery(
        query = ImportedMailCategoryFiltersQuery(
            cursor = Optional.present(null),
            isAsc = true,
        ),
    )

    internal suspend fun fetch(): Result<ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data>> {
        return graphqlClient.apolloClient.updateOperation(firstQuery) { before ->
            if (before == null) return@updateOperation fetch(cursor = null)
            if (before.user?.importedMailCategoryFilters?.isLast == true) before

            val cursor = before.user?.importedMailCategoryFilters?.cursor

            val result = fetch(cursor)
            result
        }
    }

    private suspend fun fetch(cursor: String?): ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data> {
        return graphqlClient.apolloClient.query(
            ImportedMailCategoryFiltersScreenPagingQuery(
                query = ImportedMailCategoryFiltersQuery(
                    cursor = Optional.present(cursor),
                    isAsc = true,
                ),
            ),
        ).execute()
    }
}
