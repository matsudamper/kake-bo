package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabScreenScaffoldUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class RootHomeTabScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val uiStateEvent = object : RootHomeTabScreenScaffoldUiState.Event {
        override fun onViewInitialized() {
            coroutineScope.launch {
                loginCheckUseCase.check()
            }
        }

        override fun onClickMonth() {
            coroutineScope.launch {
                viewModelEventSender.send {
                    it.navigate(RootHomeScreenStructure.Monthly())
                }
            }
        }

        override fun onClickPeriod() {
            coroutineScope.launch {
                viewModelEventSender.send {
                    it.navigate(RootHomeScreenStructure.PeriodAnalytics())
                }
            }
        }
    }

    public val uiStateFlow: StateFlow<RootHomeTabScreenScaffoldUiState> = MutableStateFlow(
        createUiState(viewModelStateFlow.value),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.value = createUiState(viewModelState)
            }
        }
    }.asStateFlow()

    public fun updateScreenStructure(current: RootHomeScreenStructure) {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                contentTYpe = when (current) {
                    is RootHomeScreenStructure.Monthly -> RootHomeTabScreenScaffoldUiState.ContentType.Monthly
                    is RootHomeScreenStructure.Period -> RootHomeTabScreenScaffoldUiState.ContentType.Period
                },
            )
        }
    }

    private fun createUiState(viewModelState: ViewModelState): RootHomeTabScreenScaffoldUiState {
        return RootHomeTabScreenScaffoldUiState(
            event = uiStateEvent,
            contentType = viewModelState.contentTYpe,
        )
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val contentTYpe: RootHomeTabScreenScaffoldUiState.ContentType = RootHomeTabScreenScaffoldUiState.ContentType.Period,
    )
}
