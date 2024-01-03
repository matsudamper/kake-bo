package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery

public class MoneyUsagesListFetchModel(
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val paging = ApolloPagingResponseCollector.create<UsageListScreenPagingQuery.Data>(
        apolloClient = apolloClient,
    )
    internal val flow = paging.flow

    internal suspend fun fetch() {
        val coroutineScope = CoroutineScope(coroutineContext)
        paging.add { collectors ->
            val cursor: String?
            when (val lastState = collectors.lastOrNull()?.flow?.value.also { println("last state -> ${it?.getSuccessOrNull()?.value?.data}") }) {
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
                    println("result -> $result")
                    if (result == null) {
                        coroutineScope.launch {
                            paging.lastRetry()
                        }
                        return@add null
                    }
                    println("hasMore -> ${result.hasMore}")
                    if (result.hasMore.not()) return@add null

                    cursor = result.cursor
                }
            }
            println("cursor -> $cursor")
            UsageListScreenPagingQuery(
                query = MoneyUsagesQuery(
                    cursor = Optional.present(cursor),
                    size = 10,
                    isAsc = false,
                ),
            )
        }
    }
}
