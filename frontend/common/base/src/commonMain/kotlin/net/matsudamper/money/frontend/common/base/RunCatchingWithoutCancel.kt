package net.matsudamper.money.frontend.common.base

import kotlinx.coroutines.CancellationException

public inline fun <T> runCatchingWithoutCancel(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
