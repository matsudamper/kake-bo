package net.matsudamper.money.backend.graphql

import graphql.execution.DataFetcherResult

class DataFetcherResultBuilder<T>(
    var value: T,
    var localContext: Any? = null,
) {
    @Suppress("UNCHECKED_CAST")
    fun build(): DataFetcherResult<T> {
        return (DataFetcherResult.newResult<Any>() as DataFetcherResult.Builder<T>)
            .data(value)
            .localContext(localContext)
            .build()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> buildNullValue(): DataFetcherResult<T>? {
            return (DataFetcherResult.newResult<Any>() as DataFetcherResult.Builder<T>)
                .data(null)
                .build()
        }

        fun <T> nullable(
            value: T?,
            localContext: Any? = null,
        ): DataFetcherResultBuilder<T?> {
            return DataFetcherResultBuilder(
                value = value,
                localContext = localContext,
            )
        }
    }
}
