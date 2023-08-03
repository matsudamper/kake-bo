package net.matsudamper.money.frontend.graphql.lib

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.graphql.GraphqlClient

class ApolloPagingResponseCollector<D : Query.Data>(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val coroutineScope: CoroutineScope,
) {
    private val collectorFlow: MutableStateFlow<List<ApolloResponseCollector<D>>> =
        MutableStateFlow(listOf())

    private val _flow: MutableStateFlow<List<ApolloResponseState<ApolloResponse<D>>>> = MutableStateFlow(listOf())
    public val flow: StateFlow<List<ApolloResponseState<ApolloResponse<D>>>> = _flow.asStateFlow()

    init {
        coroutineScope.launch {
            collectorFlow.collectLatest { collectors ->
                combine(collectors.map { it.flow }) {
                    it.toList()
                }.collectLatest {
                    _flow.value = it
                }
            }
        }
    }

    fun add(query: Query<D>, debug: String = "") {
        val collector = ApolloResponseCollector
            .create(
                apolloClient = apolloClient,
                query = query,
                fetchThrows = true,
                fetchPolicy = FetchPolicy.CacheAndNetwork,
                debug = debug,
            )
        coroutineScope.launch {
            collector.fetch()
        }
        collectorFlow.update {
            it + collector
        }
    }

    companion object {
        fun <D : Query.Data> createAndAdd(
            query: Query<D>,
            apolloClient: ApolloClient,
            coroutineScope: CoroutineScope,
        ): ApolloPagingResponseCollector<D> {
            return ApolloPagingResponseCollector<D>(
                apolloClient = apolloClient,
                coroutineScope = coroutineScope,
            ).also {
                it.add(query)
            }
        }
    }
}
