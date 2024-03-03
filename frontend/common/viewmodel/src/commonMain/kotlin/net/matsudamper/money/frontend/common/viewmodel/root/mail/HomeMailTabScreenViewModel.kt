package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeMailTabScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class HomeMailTabScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val _viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val viewModelStateFlow: StateFlow<ViewModelState> = _viewModelStateFlow.asStateFlow()

    private val navigateEventSender = EventSender<NavigateEvent>()
    public val navigateEventHandler: EventHandler<NavigateEvent> = navigateEventSender.asHandler()

    public val uiStateFlow: StateFlow<HomeMailTabScreenUiState> =
        MutableStateFlow(
            HomeMailTabScreenUiState(
                event =
                    object : HomeMailTabScreenUiState.Event {
                        override fun onClickImportTabButton() {
                            coroutineScope.launch {
                                navigateEventSender.send {
                                    it.navigate(
                                        viewModelStateFlow.value.lastImportMailStructure
                                            ?: ScreenStructure.Root.Mail.Import,
                                    )
                                }
                            }
                        }

                        override fun onClickImportedTabButton() {
                            coroutineScope.launch {
                                navigateEventSender.send {
                                    it.navigate(
                                        viewModelStateFlow.value.lastImportedMailStructure
                                            ?: ScreenStructure.Root.Mail.Imported(isLinked = false),
                                    )
                                }
                            }
                        }
                    },
            ),
        )

    public fun updateScreenStructure(structure: ScreenStructure.Root.Mail) {
        _viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                screenStructure = structure,
                lastImportedMailStructure =
                    (structure as? ScreenStructure.Root.Mail.Imported)
                        ?: viewModelState.lastImportedMailStructure,
                lastImportMailStructure =
                    (structure as? ScreenStructure.Root.Mail.Import)
                        ?: viewModelState.lastImportMailStructure,
            )
        }
    }

    public fun requestNavigate() {
        coroutineScope.launch {
            navigateEventSender.send {
                it.navigate(
                    viewModelStateFlow.value.screenStructure
                        ?: ScreenStructure.Root.Mail.Imported(isLinked = false),
                )
            }
        }
    }

    public interface NavigateEvent {
        public fun navigate(structure: ScreenStructure)
    }

    private data class ViewModelState(
        val screenStructure: ScreenStructure.Root.Mail? = null,
        val lastImportedMailStructure: ScreenStructure.Root.Mail.Imported? = null,
        val lastImportMailStructure: ScreenStructure.Root.Mail.Import? = null,
    )
}
