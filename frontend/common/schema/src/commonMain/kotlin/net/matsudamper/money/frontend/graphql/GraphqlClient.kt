package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache

object GraphqlClient {
    private val cacheFactory = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)
    val apolloClient: ApolloClient = ApolloClient.Builder()
        .serverUrl("${serverProtocol}//${serverHost}/query")
        .normalizedCache(cacheFactory)
        .build()
}
