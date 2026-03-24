package net.matsudamper.money.backend.dataloader

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import net.matsudamper.money.backend.base.OpenTelemetryInitializer
import org.dataloader.BatchLoaderEnvironment
import org.dataloader.scheduler.BatchLoaderScheduler

class OtelBatchLoaderScheduler(
    private val otelContext: Context,
    private val spanName: String,
) : BatchLoaderScheduler {
    private val tracer = OpenTelemetryInitializer.get().getTracer("graphql-dataloader")

    override fun <K, V> scheduleBatchLoader(
        scheduledCall: BatchLoaderScheduler.ScheduledBatchLoaderCall<V>,
        keys: List<K>,
        environment: BatchLoaderEnvironment,
    ): CompletableFuture<List<V>> {
        return CompletableFuture.supplyAsync(
            {
                val span = tracer.spanBuilder(spanName).startSpan()
                try {
                    span.makeCurrent().use {
                        scheduledCall.invoke().toCompletableFuture().join()
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR)
                    span.recordException(e)
                    throw e
                } finally {
                    span.end()
                }
            },
            otelContext.wrap(ForkJoinPool.commonPool()),
        )
    }

    override fun <K, V> scheduleMappedBatchLoader(
        scheduledCall: BatchLoaderScheduler.ScheduledMappedBatchLoaderCall<K, V>,
        keys: List<K>,
        environment: BatchLoaderEnvironment,
    ): CompletableFuture<Map<K, V>> {
        return CompletableFuture.supplyAsync(
            {
                val span = tracer.spanBuilder(spanName).startSpan()
                try {
                    span.makeCurrent().use {
                        scheduledCall.invoke().toCompletableFuture().join()
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR)
                    span.recordException(e)
                    throw e
                } finally {
                    span.end()
                }
            },
            otelContext.wrap(ForkJoinPool.commonPool()),
        )
    }

    override fun <K> scheduleBatchPublisher(
        scheduledCall: BatchLoaderScheduler.ScheduledBatchPublisherCall,
        keys: List<K>,
        environment: BatchLoaderEnvironment,
    ) {
        otelContext.makeCurrent().use {
            val span = tracer.spanBuilder(spanName).startSpan()
            try {
                span.makeCurrent().use {
                    scheduledCall.invoke()
                }
            } catch (e: Exception) {
                span.setStatus(StatusCode.ERROR)
                span.recordException(e)
                throw e
            } finally {
                span.end()
            }
        }
    }
}
