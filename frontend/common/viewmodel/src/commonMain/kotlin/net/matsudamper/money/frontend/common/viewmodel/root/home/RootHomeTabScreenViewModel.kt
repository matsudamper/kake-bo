package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class RootHomeTabScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
    private val loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val uiStateEvent = object : RootHomeTabUiState.Event {
        override fun onViewInitialized() {
        }
    }
    private val uiStateLoadedEvent = object : RootHomeTabUiState.LoadedEvent {
        override fun onClickMailImportButton() {
            coroutineScope.launch {
                viewModelEventSender.send { it.navigateToMailImport() }
            }
        }

        override fun onClickNotLinkedMailButton() {
            coroutineScope.launch {
                viewModelEventSender.send { it.navigateToMailLink() }
            }
        }
    }

    public val uiStateFlow: StateFlow<RootHomeTabUiState> = MutableStateFlow(
        RootHomeTabUiState(
            screenState = RootHomeTabUiState.ScreenState.Loading,
            event = uiStateEvent,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            api.getHomeScreen().collect {
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        screenState = run screenState@{
                            RootHomeTabUiState.ScreenState.Loaded(
                                notImportMailCount = it.data?.user?.userMailAttributes?.mailCount,
                                importedAndNotLinkedMailCount = it.data?.user?.importedMailAttributes?.count,
                                event = uiStateLoadedEvent,
                            )
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            loginCheckUseCase.check()
        }
    }

    public interface Event {
        public fun navigateToMailImport()
        public fun navigateToMailLink()
    }
}
