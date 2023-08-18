package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabUiState
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

    private val uiStateEvent = object : RootHomeTabUiState.Event {
        override fun onViewInitialized() {
        }

        override fun onClickMonth() {
            viewModelStateFlow.update {
                it.copy(contentTYpe = RootHomeTabUiState.ContentType.Month)
            }
        }

        override fun onClickPeriod() {
            viewModelStateFlow.update {
                it.copy(contentTYpe = RootHomeTabUiState.ContentType.Period)
            }
        }
    }

    public val uiStateFlow: StateFlow<RootHomeTabUiState> = MutableStateFlow(
        createUiState(viewModelStateFlow.value),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.value = createUiState(viewModelState)
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            loginCheckUseCase.check()
        }
    }

    private fun createUiState(viewModelState: ViewModelState): RootHomeTabUiState {
        return RootHomeTabUiState(
            event = uiStateEvent,
            contentType = viewModelState.contentTYpe,
        )
    }

    public interface Event {
        public fun navigateToMailImport()
        public fun navigateToMailLink()
    }

    private data class ViewModelState(
        val contentTYpe: RootHomeTabUiState.ContentType = RootHomeTabUiState.ContentType.Period,
    )
}
