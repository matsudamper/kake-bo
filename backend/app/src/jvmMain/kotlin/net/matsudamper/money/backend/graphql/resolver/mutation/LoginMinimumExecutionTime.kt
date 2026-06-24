package net.matsudamper.money.backend.graphql.resolver.mutation

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.delay

internal const val LOGIN_MINIMUM_EXECUTION_TIME_MILLIS: Long = 1000

@OptIn(ExperimentalContracts::class)
internal suspend fun <T> minExecutionTime(
    minMillSecond: Long,
    block: suspend () -> T,
): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val startTime = System.currentTimeMillis()
    val result = block()
    while (true) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - startTime >= minMillSecond) {
            break
        }
        delay(10)
    }

    return result
}
