package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.exception.CacheMissException

suspend fun <D : Operation.Data> ApolloClient.updateOperation(query: Operation<D>, block: suspend (D?) -> ApolloResponse<D>?): Result<ApolloResponse<D>> {
    return runCatching {
        while (true) {
            val before = readOperationOrNull(query)
            val newResponse = block(before) ?: break
            val new = newResponse.data ?: return@runCatching newResponse

            if (readOperationOrNull(query) == before) {
                apolloStore.writeOperation(
                    operation = query,
                    operationData = new,
                    customScalarAdapters = customScalarAdapters,
                    publish = true,
                )
                return@runCatching newResponse
            }
        }
        throw IllegalStateException()
    }
}

private fun <D : Operation.Data> ApolloClient.readOperationOrNull(query: Operation<D>): D? {
    return try {
        apolloStore.readOperation(query, customScalarAdapters)
    } catch (_: CacheMissException) {
        null
    }
}
