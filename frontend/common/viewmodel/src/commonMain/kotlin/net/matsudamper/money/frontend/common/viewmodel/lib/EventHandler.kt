package net.matsudamper.money.frontend.common.viewmodel.lib

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

public class EventHandler<Receiver>(
    private val events: ReceiveChannel<suspend (Receiver) -> Unit>,
) {
    public suspend fun collect(target: Receiver) {
        events.receiveAsFlow().collect { block ->
            block(target)
        }
    }
}

public class EventHandler2<
    Receiver1, Handler1 : EventHandler<Receiver1>,
    Receiver2, Handler2 : EventHandler<Receiver2>,
    >(
    private val handler1: Handler1,
    private val handler2: Handler2,
) {
    public suspend fun collect(
        receiver: Receiver1,
        receiver2: Receiver2,
    ) {
        coroutineScope {
            launch { handler1.collect(receiver) }
            launch { handler2.collect(receiver2) }
        }
    }
}

public class EventHandler3<
    Receiver1, Handler1 : EventHandler<Receiver1>,
    Receiver2, Handler2 : EventHandler<Receiver2>,
    Receiver3, Handler3 : EventHandler<Receiver3>,
    >(
    private val handler1: Handler1,
    private val handler2: Handler2,
    private val handler3: Handler3,
) {
    public suspend fun collect(
        receiver: Receiver1,
        receiver2: Receiver2,
        receiver3: Receiver3,
    ) {
        coroutineScope {
            launch { handler1.collect(receiver) }
            launch { handler2.collect(receiver2) }
            launch { handler3.collect(receiver3) }
        }
    }
}
