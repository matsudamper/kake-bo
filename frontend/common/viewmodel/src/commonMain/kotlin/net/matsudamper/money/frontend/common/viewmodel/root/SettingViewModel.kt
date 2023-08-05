package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.RootSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

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

                override fun onClickMailFilter() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigate(
                                ScreenStructure.Root.Settings.MailCategoryFilter,
                            )
                        }
                    }
                }
            },
        ),
    ).asStateFlow()

    public interface Event {
        public fun navigateToImapConfig()
        public fun navigateToCategoriesConfig()
        public fun navigate(structure: ScreenStructure)
    }
}
