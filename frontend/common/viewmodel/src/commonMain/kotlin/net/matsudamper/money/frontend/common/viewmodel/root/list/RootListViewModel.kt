package net.matsudamper.money.frontend.common.viewmodel.root.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.ui.screen.RootListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class RootListViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    public val uiStateFlow: StateFlow<RootListScreenUiState> = MutableStateFlow(
        RootListScreenUiState(
            event = object : RootListScreenUiState.Event {
                override fun onClickAdd() {
                    coroutineScope.launch {
                        viewModelEventSender.send {
                            it.navigateToAddMoneyUsage()
                        }
                    }
                }
            },
        ),
    ).asStateFlow()

    public interface Event {
        public fun navigateToAddMoneyUsage()
    }
}