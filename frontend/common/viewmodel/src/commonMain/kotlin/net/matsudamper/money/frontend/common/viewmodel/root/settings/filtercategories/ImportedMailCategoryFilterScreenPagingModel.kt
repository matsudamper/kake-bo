package net.matsudamper.money.frontend.common.viewmodel.root.settings.filtercategories

import kotlinx.coroutines.CoroutineScope
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery

public class ImportedMailCategoryFilterScreenPagingModel(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val coroutineScope: CoroutineScope,
) {
    private val pagingState = ApolloPagingResponseCollector.create<ImportedMailCategoryFiltersScreenPagingQuery.Data>(
        apolloClient = apolloClient,
        coroutineScope = coroutineScope,
    )

    internal val flow = pagingState.flow

    internal fun clear() {
        pagingState.clear()
    }

    internal fun fetch() {
        val last = pagingState.flow.value.lastOrNull()

        val cursor: String?
        when (last) {
            is ApolloResponseState.Loading -> {
                return
            }

            is ApolloResponseState.Failure -> {
                pagingState.lastRetry()
                return
            }

            is ApolloResponseState.Success -> {
                val response = last.value.data?.user?.importedMailCategoryFilters
                if (response == null) {
                    pagingState.lastRetry()
                    return
                }
                if (response.isLast) {
                    return
                }

                cursor = response.cursor
            }

            null -> {
                cursor = null
            }
        }

        pagingState.add(
            query = ImportedMailCategoryFiltersScreenPagingQuery(
                query = ImportedMailCategoryFiltersQuery(
                    cursor = Optional.present(cursor),
                    isAsc = true,
                ),
            ),
        )
    }
}
