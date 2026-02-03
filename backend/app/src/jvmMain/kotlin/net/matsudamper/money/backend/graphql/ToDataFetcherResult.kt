package net.matsudamper.money.backend.graphql

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult

@Suppress("UNCHECKED_CAST")
fun <T, S : CompletionStage<T>> S.toDataFetcher(): CompletionStage<DataFetcherResult<T>> {
    return thenApply {
        (DataFetcherResult.newResult<Any>() as DataFetcherResult.Builder<T>)
            .data(it)
            .build()
    }
}
