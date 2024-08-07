package net.matsudamper.money.frontend.graphql.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch

public class ApolloResponseCollector<D : Query.Data>(
    private val apolloClient: ApolloClient,
    private val query: Query<D>,
    private val fetchThrows: Boolean,
    private val refetchThrows: Boolean,
    private val fetchPolicy: FetchPolicy,
) {
    private val _flow: MutableStateFlow<ApolloResponseState<ApolloResponse<D>>> =
        MutableStateFlow(
            ApolloResponseState.loading(),
        )
    public val flow: StateFlow<ApolloResponseState<ApolloResponse<D>>> = _flow.asStateFlow()

    fun getFlow(): StateFlow<ApolloResponseState<ApolloResponse<D>>> = flow

    private var job: Job = Job()

    suspend fun fetch() {
        fetch(CoroutineScope(currentCoroutineContext()))
    }

    public fun fetch(coroutineScope: CoroutineScope) {
        job.cancel()
        job =
            coroutineScope.launch {
                apolloClient
                    .query(query)
                    .fetchPolicy(fetchPolicy)
                    .watch(
                        fetchThrows = fetchThrows,
                        refetchThrows = refetchThrows,
                    )
                    .catch {
                        it.printStackTrace()
                        _flow.value = ApolloResponseState.failure(it)
                    }
                    .collect {
                        println("collect: Data(${it.data})")
                        _flow.value = ApolloResponseState.success(it)
                    }
            }
    }

    public fun cancel() {
        job.cancel()
    }

    companion object {
        fun <D : Query.Data> create(
            apolloClient: ApolloClient,
            query: Query<D>,
            fetchThrows: Boolean = true,
            refetchThrows: Boolean = false,
            fetchPolicy: FetchPolicy = FetchPolicy.CacheAndNetwork,
        ): ApolloResponseCollector<D> {
            return ApolloResponseCollector(
                apolloClient = apolloClient,
                query = query,
                fetchThrows = fetchThrows,
                refetchThrows = refetchThrows,
                fetchPolicy = fetchPolicy,
            )
        }
    }
}
