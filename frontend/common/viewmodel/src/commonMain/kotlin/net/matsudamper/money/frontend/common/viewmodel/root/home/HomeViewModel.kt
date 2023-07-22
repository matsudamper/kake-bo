package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.root.HomeScreenUiState

public class HomeViewModel(
    private val coroutineScope: CoroutineScope,
    private val homeGraphqlApi: HomeGraphqlApi,
    private val loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val uiStateEvent = object : HomeScreenUiState.Event {
        override fun onViewInitialized() {

        }
    }
    private val uiStateLoadedEvent = object : HomeScreenUiState.LoadedEvent {
        override fun onClickMailImport() {
            coroutineScope.launch {
                viewModelEventSender.send { it.navigateToMailImport() }
            }
        }
    }

    public val uiStateFlow: StateFlow<HomeScreenUiState> = MutableStateFlow(
        HomeScreenUiState(
            screenState = HomeScreenUiState.ScreenState.Loading,
            event = uiStateEvent,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            homeGraphqlApi.getHomeScreen().collect {
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        screenState = run screenState@{
                            HomeScreenUiState.ScreenState.Loaded(
                                notImportMailCount = it.data?.user?.userMailAttributes?.mailCount,
                                event = uiStateLoadedEvent
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
    }
}
