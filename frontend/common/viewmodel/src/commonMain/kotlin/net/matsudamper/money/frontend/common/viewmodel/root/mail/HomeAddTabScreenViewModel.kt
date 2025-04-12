package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.mail.HomeAddTabScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class HomeAddTabScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val _viewModelStateFlow = MutableStateFlow(ViewModelState())
    private val viewModelStateFlow: StateFlow<ViewModelState> = _viewModelStateFlow.asStateFlow()

    private val navigateEventSender = EventSender<NavigateEvent>()
    public val navigateEventHandler: EventHandler<NavigateEvent> = navigateEventSender.asHandler()

    public val uiStateFlow: StateFlow<HomeAddTabScreenUiState> = MutableStateFlow(
        HomeAddTabScreenUiState(
            rootScreenScaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {},
            event = object : HomeAddTabScreenUiState.Event {
                override fun onClickImportButton() {
                    viewModelScope.launch {
                        navigateEventSender.send {
                            it.navigate(
                                viewModelStateFlow.value.lastImportMailStructure
                                    ?: ScreenStructure.Root.Add.Import,
                            )
                        }
                    }
                }

                override fun onClickImportedButton() {
                    viewModelScope.launch {
                        navigateEventSender.send {
                            it.navigate(
                                viewModelStateFlow.value.lastImportedMailStructure
                                    ?: ScreenStructure.Root.Add.Imported(isLinked = false),
                            )
                        }
                    }
                }
            },
        ),
    )

    public fun updateScreenStructure(structure: ScreenStructure.Root.Add) {
        _viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                screenStructure = structure,
                lastImportedMailStructure = (structure as? ScreenStructure.Root.Add.Imported)
                    ?: viewModelState.lastImportedMailStructure,
                lastImportMailStructure = (structure as? ScreenStructure.Root.Add.Import)
                    ?: viewModelState.lastImportMailStructure,
            )
        }
    }

    public fun requestNavigate() {
        viewModelScope.launch {
            navigateEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Add.Root,
                )
            }
        }
    }

    public interface NavigateEvent {
        public fun navigate(structure: ScreenStructure)
    }

    private data class ViewModelState(
        val screenStructure: ScreenStructure.Root.Add? = null,
        val lastImportedMailStructure: ScreenStructure.Root.Add.Imported? = null,
        val lastImportMailStructure: ScreenStructure.Root.Add.Import? = null,
    )
}
