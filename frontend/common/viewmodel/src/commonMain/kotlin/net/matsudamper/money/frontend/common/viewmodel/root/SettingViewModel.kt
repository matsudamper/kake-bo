package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.settings.RootSettingScreenUiState
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class SettingViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val ioDispatchers: CoroutineDispatcher,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelState = MutableStateFlow<ViewModelState>(
        ViewModelState(),
    )
    private val backgroundEventSender = EventSender<Event>()
    public val backgroundEventHandler: EventHandler<Event> = backgroundEventSender.asHandler()

    public val uiState: StateFlow<RootSettingScreenUiState> = MutableStateFlow(
        RootSettingScreenUiState(
            kotlinVersion = KotlinVersion.CURRENT.toString(),
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            event = object : RootSettingScreenUiState.Event {
                override fun onResume() {
                }

                override fun onClickLoginSetting() {
                    viewModelScope.launch {
                        backgroundEventSender.send {
                            it.navigate(
                                ScreenStructure.Root.Settings.Login,
                            )
                        }
                    }
                }

                override fun onClickImapButton() {
                    viewModelScope.launch {
                        backgroundEventSender.send {
                            it.navigateToImapConfig()
                        }
                    }
                }

                override fun onClickCategoryButton() {
                    viewModelScope.launch {
                        backgroundEventSender.send {
                            it.navigateToCategoriesConfig()
                        }
                    }
                }

                override fun onClickApiSetting() {
                    viewModelScope.launch {
                        backgroundEventSender.send {
                            it.navigateToApiSetting()
                        }
                    }
                }

                override fun onClickMailFilter() {
                    viewModelScope.launch {
                        backgroundEventSender.send {
                            it.navigate(
                                ScreenStructure.Root.Settings.MailCategoryFilters,
                            )
                        }
                    }
                }

                override fun onClickGitHub() {
                    viewModelScope.launch {
                        backgroundEventSender.send {
                            it.open("https://github.com/matsudamper/kake-bo")
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

    public fun requestNavigate() {
        viewModelScope.launch {
            backgroundEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Settings.Root,
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
        public fun navigateToApiSetting()

        public fun navigate(structure: ScreenStructure)

        public fun open(url: String)
    }
}
