package net.matsudamper.money.frontend.graphql.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import net.matsudamper.money.frontend.graphql.GraphqlClient

class ApolloPagingResponseCollector<D : Query.Data>(
    private val graphqlClient: GraphqlClient,
    private val coroutineScope: CoroutineScope,
    private val fetchPolicy: FetchPolicy = FetchPolicy.NetworkOnly,
) {
    private val collectorFlow: MutableStateFlow<List<ApolloResponseCollector<D>>> = MutableStateFlow(listOf())

    private val mutableStateFlow: MutableStateFlow<List<ApolloResponseState<ApolloResponse<D>>>> = MutableStateFlow(listOf())

    fun getFlow(): StateFlow<List<ApolloResponseState<ApolloResponse<D>>>> = mutableStateFlow.asStateFlow()

    init {
        coroutineScope.launch {
            collectorFlow.collectLatest { collectors ->
                combine(collectors.map { it.getFlow() }) {
                    it.toList()
                }.collectLatest {
                    mutableStateFlow.value = it
                }
            }
        }
    }

    fun lastRetry() {
        coroutineScope.launch {
            when (collectorFlow.value.lastOrNull()?.getFlow()?.value) {
                is ApolloResponseState.Failure -> collectorFlow.value.lastOrNull()?.fetch(this)
                is ApolloResponseState.Loading,
                is ApolloResponseState.Success,
                null,
                -> Unit
            }
        }
    }

    fun add(queryBlock: (List<ApolloResponseCollector<D>>) -> Query<D>?) {
        var collector: ApolloResponseCollector<D>? = null
        collectorFlow.update { collectors ->
            val query = queryBlock(collectors) ?: return

            val tmp = ApolloResponseCollector
                .create(
                    apolloClient = graphqlClient.apolloClient,
                    query = query,
                    fetchPolicy = fetchPolicy,
                )
            collector = tmp
            collectors + tmp
        }
        coroutineScope.launch {
            collector?.fetch(this)
        }
    }

    fun clear() {
        collectorFlow.value.map {
            it.cancel()
        }
        collectorFlow.update {
            listOf()
        }
        mutableStateFlow.update {
            listOf()
        }
    }

    companion object {
        fun <D : Query.Data> create(
            graphqlClient: GraphqlClient,
            coroutineScope: CoroutineScope,
            fetchPolicy: FetchPolicy = FetchPolicy.NetworkOnly,
        ): ApolloPagingResponseCollector<D> {
            return ApolloPagingResponseCollector(
                graphqlClient = graphqlClient,
                coroutineScope = coroutineScope,
                fetchPolicy = fetchPolicy,
            )
        }
    }
}
