package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import net.matsudamper.money.backend.graphql.otelSupplyAsync

internal const val LOGIN_MINIMUM_EXECUTION_TIME_MILLIS: Long = 1000

/**
 * 残り時間の待機でスレッドを占有しないよう、完了の遅延は delayedExecutor で行う。
 * スレッドを占有すると並列リクエストで共有スレッドプールが枯渇し、他の処理まで止まるため。
 */
internal fun <T> minExecutionTime(
    minMillSecond: Long,
    block: () -> T,
): CompletableFuture<T> {
    val startTime = System.currentTimeMillis()
    return otelSupplyAsync(block).thenCompose { result ->
        val remainingMillis = minMillSecond - (System.currentTimeMillis() - startTime)
        if (remainingMillis <= 0) {
            CompletableFuture.completedFuture(result)
        } else {
            CompletableFuture.supplyAsync(
                { result },
                CompletableFuture.delayedExecutor(remainingMillis, TimeUnit.MILLISECONDS),
            )
        }
    }
}
