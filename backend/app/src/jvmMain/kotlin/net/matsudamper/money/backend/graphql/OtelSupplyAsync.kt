package net.matsudamper.money.backend.graphql

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import io.opentelemetry.context.Context

internal fun <T> otelSupplyAsync(supplier: () -> T): CompletableFuture<T> {
    val context = Context.current()
    return CompletableFuture.supplyAsync(
        { context.makeCurrent().use { supplier() } },
        ForkJoinPool.commonPool(),
    )
}
