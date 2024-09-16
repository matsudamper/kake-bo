package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter

public class MoneyUsagesListFetchModel(
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
    coroutineScope: CoroutineScope,
) {
    private val state = MutableStateFlow(State())
    private val paging =
        ApolloPagingResponseCollector.create<UsageListScreenPagingQuery.Data>(
            apolloClient = apolloClient,
            coroutineScope = coroutineScope,
        )

    private val pagingFlow =
        MutableStateFlow(
            ApolloPagingResponseCollector.create<UsageListScreenPagingQuery.Data>(
                apolloClient = apolloClient,
                coroutineScope = coroutineScope,
            ),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal suspend fun getFlow(): Flow<List<ApolloResponseState<ApolloResponse<UsageListScreenPagingQuery.Data>>>> {
        return flow {
            emitAll(
                pagingFlow.mapLatest { collectors ->
                    collectors.getFlow()
                }.flattenMerge(),
            )
        }
    }

    internal suspend fun fetch() {
        val coroutineScope = CoroutineScope(coroutineContext)
        paging.add { collectors ->
            val cursor: String?
            when (val lastState = collectors.lastOrNull()?.getFlow()?.value) {
                is ApolloResponseState.Loading -> return@add null
                is ApolloResponseState.Failure -> {
                    coroutineScope.launch {
                        paging.lastRetry()
                    }
                    return@add null
                }

                null -> {
                    cursor = null
                }

                is ApolloResponseState.Success -> {
                    val result = lastState.value.data?.user?.moneyUsages
                    if (result == null) {
                        coroutineScope.launch {
                            paging.lastRetry()
                        }
                        return@add null
                    }
                    if (result.hasMore.not()) return@add null

                    cursor = result.cursor
                }
            }
            UsageListScreenPagingQuery(
                query =
                MoneyUsagesQuery(
                    cursor = Optional.present(cursor),
                    size = 10,
                    isAsc = false,
                    filter =
                    Optional.present(
                        MoneyUsagesQueryFilter(
                            text = Optional.present(state.value.searchText),
                        ),
                    ),
                ),
            )
        }
    }

    public fun changeText(searchText: String?) {
        if (state.value.searchText == searchText) return
        state.update {
            it.copy(
                searchText = searchText,
            )
        }
    }

    private data class State(
        val searchText: String? = null,
    )
}
