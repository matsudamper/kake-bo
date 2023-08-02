package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.ui.screen.root.RootSettingScreenUiState

public class SettingViewModel(
    private val coroutineScope: CoroutineScope,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val ioDispatchers: CoroutineDispatcher,
) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiState: StateFlow<RootSettingScreenUiState> = MutableStateFlow(
        RootSettingScreenUiState(
            event = object : RootSettingScreenUiState.Event {
                override fun onResume() {
                }

                override fun onClickImapButton() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigateToImapConfig()
                        }
                    }
                }

                override fun onClickCategoryButton() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigateToCategoriesConfig()
                        }
                    }
                }
            },
        ),
    ).asStateFlow()

    public interface Event {
        public fun navigateToImapConfig()
        public fun navigateToCategoriesConfig()
    }
}
