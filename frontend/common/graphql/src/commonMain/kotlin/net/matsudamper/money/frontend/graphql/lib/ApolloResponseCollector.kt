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
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.apollographql.apollo3.cache.normalized.watch

public class ApolloResponseCollector<D : Query.Data>(
    private val apolloClient: ApolloClient,
    private val query: Query<D>,
    private val fetchPolicy: FetchPolicy,
) {
    private val mutableStateFlow: MutableStateFlow<ApolloResponseState<ApolloResponse<D>>> =
        MutableStateFlow(
            ApolloResponseState.loading(),
        )

    fun getFlow(): StateFlow<ApolloResponseState<ApolloResponse<D>>> = mutableStateFlow.asStateFlow()

    private var job: Job = Job()

    suspend fun fetch() {
        fetch(CoroutineScope(currentCoroutineContext()))
    }

    public fun fetch(coroutineScope: CoroutineScope) {
        job.cancel()
        job = coroutineScope.launch {
            apolloClient
                .query(query)
                .fetchPolicy(fetchPolicy)
                .watch()
                .catch {
                    it.printStackTrace()
                    mutableStateFlow.value = ApolloResponseState.failure(it)
                }
                .collect {
                    // 最初のロードでネットワークから情報が来る前にdataにnullが来る事がある
                    if (it.isFromCache && it.data == null) return@collect
                    mutableStateFlow.value = ApolloResponseState.success(it)
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
            fetchPolicy: FetchPolicy = FetchPolicy.CacheAndNetwork,
        ): ApolloResponseCollector<D> {
            return ApolloResponseCollector(
                apolloClient = apolloClient,
                query = query,
                fetchPolicy = fetchPolicy,
            )
        }
    }
}
