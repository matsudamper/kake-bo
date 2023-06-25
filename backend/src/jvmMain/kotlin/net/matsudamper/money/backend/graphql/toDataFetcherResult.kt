package net.matsudamper.money.backend.graphql

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult

fun <T, S : CompletionStage<T>> S.toDataFetcher(): CompletionStage<DataFetcherResult<T>> {
    return thenApply {
        DataFetcherResult.newResult<T>()
            .data(it)
            .build()
    }
}
