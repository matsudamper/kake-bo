package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val viewModelState = MutableStateFlow<ViewModelState>(
        ViewModelState(),
    )
    private val backgroundEventSender = EventSender<Event>()
    public val backgroundEventHandler: EventHandler<Event> = backgroundEventSender.asHandler()

    public val uiState: StateFlow<RootSettingScreenUiState> = MutableStateFlow(
        RootSettingScreenUiState(
            event = object : RootSettingScreenUiState.Event {
                override fun onResume() {
                }

                override fun onClickImapButton() {
                    coroutineScope.launch {
                        backgroundEventSender.send {
                            it.navigateToImapConfig()
                        }
                    }
                }

                override fun onClickCategoryButton() {
                    coroutineScope.launch {
                        backgroundEventSender.send {
                            it.navigateToCategoriesConfig()
                        }
                    }
                }

                override fun onClickMailFilter() {
                    coroutineScope.launch {
                        backgroundEventSender.send {
                            it.navigate(
                                ScreenStructure.Root.Settings.MailCategoryFilters,
                            )
                        }
                    }
                }
            },
        ),
    ).asStateFlow()

    public fun updateLastStructure(state: ScreenStructure.Root.Settings) {
        viewModelState.update {
            it.copy(
                latestStructure = state,
            )
        }
    }

    public fun requestNavigate(currentScreen: ScreenStructure) {
        coroutineScope.launch {
            backgroundEventSender.send {
                it.navigate(
                    if (currentScreen is ScreenStructure.Root.Settings) {
                        ScreenStructure.Root.Settings.Root
                    } else {
                        viewModelState.value.latestStructure
                            ?: ScreenStructure.Root.Settings.Root
                    },
                )
            }
        }
    }

    private data class ViewModelState(
        val latestStructure: ScreenStructure.Root.Settings? = null,
    )

    public interface Event {
        public fun navigateToImapConfig()
        public fun navigateToCategoriesConfig()
        public fun navigate(structure: ScreenStructure)
    }
}
