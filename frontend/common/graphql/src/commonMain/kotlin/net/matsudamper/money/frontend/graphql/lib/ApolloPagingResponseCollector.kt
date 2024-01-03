package net.matsudamper.money.frontend.graphql.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import net.matsudamper.money.frontend.graphql.GraphqlClient

class ApolloPagingResponseCollector<D : Query.Data>(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val fetchPolicy: FetchPolicy = FetchPolicy.NetworkOnly,
) {
    private val collectorFlow: MutableStateFlow<List<ApolloResponseCollector<D>>> =
        MutableStateFlow(listOf())

    private val _flow: MutableStateFlow<List<ApolloResponseState<ApolloResponse<D>>>> = MutableStateFlow(listOf())

    @OptIn(FlowPreview::class)
    fun getFlow(): Flow<List<ApolloResponseState<ApolloResponse<D>>>> {
        return collectorFlow
            .map { collectors ->
                combine(collectors.map { it.flow }) { it.toList() }
            }.flattenMerge()
    }

    val lastValue: List<ApolloResponseState<ApolloResponse<D>>> get() = collectorFlow.value.map { it.flow.value }

    suspend fun lastRetry() {
        when (collectorFlow.value.lastOrNull()?.flow?.value) {
            is ApolloResponseState.Failure -> collectorFlow.value.lastOrNull()?.fetch()
            is ApolloResponseState.Loading,
            is ApolloResponseState.Success,
            null,
            -> Unit
        }
    }

    suspend fun add(
        queryBlock: (List<ApolloResponseCollector<D>>) -> Query<D>?,
    ) {
        var collector: ApolloResponseCollector<D>? = null
        collectorFlow.update { collectors ->
            val query = queryBlock(collectors) ?: return

            val tmp = ApolloResponseCollector
                .create(
                    apolloClient = apolloClient,
                    query = query,
                    fetchThrows = true,
                    fetchPolicy = fetchPolicy,
                )
            collector = tmp
            collectors + tmp
        }
        CoroutineScope(currentCoroutineContext()).launch {
            collector?.fetch()
        }
    }

    fun clear() {
        collectorFlow.value.map {
            it.cancel()
        }
        collectorFlow.update {
            listOf()
        }
        _flow.update {
            listOf()
        }
    }

    companion object {
        fun <D : Query.Data> create(
            apolloClient: ApolloClient,
            fetchPolicy: FetchPolicy = FetchPolicy.NetworkOnly,
        ): ApolloPagingResponseCollector<D> {
            return ApolloPagingResponseCollector(
                apolloClient = apolloClient,
                fetchPolicy = fetchPolicy,
            )
        }
    }
}
