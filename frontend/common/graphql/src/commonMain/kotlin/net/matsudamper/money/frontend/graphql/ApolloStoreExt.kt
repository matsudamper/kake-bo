package net.matsudamper.money.frontend.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.exception.CacheMissException

sealed interface UpdateOperationResponseResult<D : Operation.Data> {
    data class Success<D : Operation.Data>(val result: ApolloResponse<D>) : UpdateOperationResponseResult<D>
    data class Error<D : Operation.Data>(val e: Throwable?) : UpdateOperationResponseResult<D>
    class NoHasMore<D : Operation.Data> : UpdateOperationResponseResult<D>

    fun isSuccess(): Boolean = this is Success<D>
    fun getOrNull(): D? = (this as? Success<D>)?.result?.data
}

class UpdateOperationResponseScope<D : Operation.Data> {
    fun success(result: ApolloResponse<D>): UpdateOperationResponseResult.Success<D> {
        return UpdateOperationResponseResult.Success(result)
    }

    fun error(): UpdateOperationResponseResult.Error<D> {
        return UpdateOperationResponseResult.Error(null)
    }

    fun noHasMore(): UpdateOperationResponseResult.NoHasMore<D> {
        return UpdateOperationResponseResult.NoHasMore()
    }
}

suspend fun <D : Operation.Data> ApolloClient.updateOperation(
    cacheQueryKey: Operation<D>,
    block: suspend UpdateOperationResponseScope<D>.(D?) -> UpdateOperationResponseResult<D>,
): UpdateOperationResponseResult<D> {
    val before = readOperationOrNull(cacheQueryKey)
    return runCatching {
        block(UpdateOperationResponseScope(), before)
    }.fold(
        onFailure = {
            UpdateOperationResponseResult.Error(it)
        },
        onSuccess = {
            when (it) {
                is UpdateOperationResponseResult.NoHasMore<D> -> Unit
                is UpdateOperationResponseResult.Error<D> -> Unit
                is UpdateOperationResponseResult.Success<D> -> {
                    val data = it.result.data ?: return@fold UpdateOperationResponseResult.Error(
                        NullPointerException("ApolloResponse.data is null"),
                    )
                    apolloStore.writeOperation(
                        operation = cacheQueryKey,
                        operationData = data,
                        customScalarAdapters = customScalarAdapters,
                        publish = true,
                    )
                }
            }
            it
        },
    )
}

private fun <D : Operation.Data> ApolloClient.readOperationOrNull(query: Operation<D>): D? {
    return try {
        apolloStore.readOperation(query, customScalarAdapters)
    } catch (_: CacheMissException) {
        null
    }
}
