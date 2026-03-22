package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import io.opentelemetry.context.Context
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.scheduler.BatchLoaderScheduler

class OtelBatchLoaderScheduler : BatchLoaderScheduler {

    override fun <K, V> scheduleBatchLoader(
        scheduledCall: BatchLoaderScheduler.ScheduledBatchLoaderCall<V>,
        keys: List<K>,
        environment: BatchLoaderEnvironment,
    ): CompletableFuture<List<V>> {
        val capturedContext = Context.current()
        return CompletableFuture.supplyAsync(
            { scheduledCall.invoke().toCompletableFuture().join() },
            capturedContext.wrap(ForkJoinPool.commonPool()),
        )
    }

    override fun <K, V> scheduleMappedBatchLoader(
        scheduledCall: BatchLoaderScheduler.ScheduledMappedBatchLoaderCall<K, V>,
        keys: List<K>,
        environment: BatchLoaderEnvironment,
    ): CompletableFuture<Map<K, V>> {
        val capturedContext = Context.current()
        return CompletableFuture.supplyAsync(
            { scheduledCall.invoke().toCompletableFuture().join() },
            capturedContext.wrap(ForkJoinPool.commonPool()),
        )
    }

    override fun <K> scheduleBatchPublisher(
        scheduledCall: BatchLoaderScheduler.ScheduledBatchPublisherCall,
        keys: List<K>,
        environment: BatchLoaderEnvironment,
    ) {
        scheduledCall.invoke()
    }
}
