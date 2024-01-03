package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery


public class MoneyUsagesListFetchModel(
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
    coroutineScope: CoroutineScope,
) {
    private val paging = ApolloPagingResponseCollector.create<UsageListScreenPagingQuery.Data>(
        apolloClient = apolloClient,
        coroutineScope = coroutineScope,
    )
    internal val flow = paging.flow

    internal fun fetch() {
        paging.add { collectors ->
            val cursor: String?
            when (val lastState = collectors.lastOrNull()?.flow?.value) {
                is ApolloResponseState.Loading -> return@add null
                is ApolloResponseState.Failure -> {
                    paging.lastRetry()
                    return@add null
                }

                null -> {
                    cursor = null
                }

                is ApolloResponseState.Success -> {
                    val result = lastState.value.data?.user?.moneyUsages
                    if (result == null) {
                        paging.lastRetry()
                        return@add null
                    }
                    if (result.hasMore.not()) return@add null

                    cursor = result.cursor
                }
            }
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
