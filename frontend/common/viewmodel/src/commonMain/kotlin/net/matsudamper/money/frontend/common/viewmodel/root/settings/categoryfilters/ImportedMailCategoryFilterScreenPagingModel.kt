package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
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
    private val pagingState =
        ApolloPagingResponseCollector<ImportedMailCategoryFiltersScreenPagingQuery.Data>(
            apolloClient = apolloClient,
            coroutineScope = coroutineScope,
        )

    internal suspend fun getFlow(): Flow<List<ApolloResponseState<ApolloResponse<ImportedMailCategoryFiltersScreenPagingQuery.Data>>>> {
        return flow {
            emitAll(
                combine(pagingState.getFlow()) {
                    it.toList().flatten()
                },
            )
        }
    }

    internal fun clear() {
        pagingState.clear()
    }

    internal fun fetch() {
        pagingState.add(
            queryBlock = {
                val cursor: String?
                when (val last = it.lastOrNull()?.getFlow()?.value) {
                    is ApolloResponseState.Loading -> {
                        return@add null
                    }

                    is ApolloResponseState.Failure -> {
                        coroutineScope.launch {
                            pagingState.lastRetry()
                        }
                        return@add null
                    }

                    is ApolloResponseState.Success -> {
                        val response = last.value.data?.user?.importedMailCategoryFilters
                        if (response == null) {
                            coroutineScope.launch {
                                pagingState.lastRetry()
                            }
                            return@add null
                        }
                        if (response.isLast) {
                            return@add null
                        }

                        cursor = response.cursor
                    }

                    null -> {
                        cursor = null
                    }
                }

                ImportedMailCategoryFiltersScreenPagingQuery(
                    query =
                    ImportedMailCategoryFiltersQuery(
                        cursor = Optional.present(cursor),
                        isAsc = true,
                    ),
                )
            },
        )
    }
}
