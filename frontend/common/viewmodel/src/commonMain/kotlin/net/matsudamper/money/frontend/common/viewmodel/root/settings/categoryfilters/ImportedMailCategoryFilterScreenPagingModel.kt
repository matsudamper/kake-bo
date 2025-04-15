package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailCategoryFiltersScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.ImportedMailCategoryFiltersQuery

public class ImportedMailCategoryFilterScreenPagingModel(
    scopedObjectFeature: ScopedObjectFeature,
    graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val pagingState = ApolloPagingResponseCollector<ImportedMailCategoryFiltersScreenPagingQuery.Data>(
        graphqlClient = graphqlClient,
        coroutineScope = viewModelScope,
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
                        viewModelScope.launch {
                            pagingState.lastRetry()
                        }
                        return@add null
                    }

                    is ApolloResponseState.Success -> {
                        val response = last.value.data?.user?.importedMailCategoryFilters
                        if (response == null) {
                            viewModelScope.launch {
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
                    query = ImportedMailCategoryFiltersQuery(
                        cursor = Optional.present(cursor),
                        isAsc = true,
                    ),
                )
            },
        )
    }
}
