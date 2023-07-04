package net.matsudamper.money.frontend.common.viewmodel.lib

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.receiveAsFlow

public class EventHandler<Receiver>(
    private val events: ReceiveChannel<suspend (Receiver) -> Unit>,
) {
    public suspend fun collect(target: Receiver) {
        events.receiveAsFlow().collect { block ->
            block(target)
        }
    }
}