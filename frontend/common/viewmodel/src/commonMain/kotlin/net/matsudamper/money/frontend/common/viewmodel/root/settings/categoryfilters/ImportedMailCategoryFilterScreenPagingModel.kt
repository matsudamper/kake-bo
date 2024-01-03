package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery

public class ImportedMailCategoryFilterScreenPagingModel(
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val coroutineScope: CoroutineScope,
) {
    private val pagingState = ApolloPagingResponseCollector<ImportedMailCategoryFiltersScreenPagingQuery.Data>(
        apolloClient = apolloClient,
    )

    internal val flow = pagingState.flow

    internal fun clear() {
        pagingState.clear()
    }

    internal suspend fun fetch() {
        val last = pagingState.lastValue.lastOrNull()

        val cursor: String?
        when (last) {
            is ApolloResponseState.Loading -> {
                return
            }

            is ApolloResponseState.Failure -> {
                coroutineScope.launch {
                    pagingState.lastRetry()
                }
                return
            }

            is ApolloResponseState.Success -> {
                val response = last.value.data?.user?.importedMailCategoryFilters
                if (response == null) {
                    coroutineScope.launch {
                        pagingState.lastRetry()
                    }
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
            queryBlock = {
                ImportedMailCategoryFiltersScreenPagingQuery(
                    query = ImportedMailCategoryFiltersQuery(
                        cursor = Optional.present(cursor),
                        isAsc = true,
                    ),
                )
            },
        )
    }
}
