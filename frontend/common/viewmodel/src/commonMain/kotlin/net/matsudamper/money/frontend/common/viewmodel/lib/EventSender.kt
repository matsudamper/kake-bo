package net.matsudamper.money.frontend.common.viewmodel.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

public class EventSender<Receiver> {
    private val receiverChannel = Channel<suspend (Receiver) -> Unit>(Channel.UNLIMITED)
    public suspend fun <R> send(block: suspend (Receiver) -> R): R {
        val scope = CoroutineScope(coroutineContext)
        return suspendCoroutine { continuation ->
            scope.launch {
                receiverChannel.send { receiver ->
                    continuation.resume(block(receiver))
                }
            }
        }
    }

    public fun asHandler(): EventHandler<Receiver> {
        return EventHandler(receiverChannel)
    }
}