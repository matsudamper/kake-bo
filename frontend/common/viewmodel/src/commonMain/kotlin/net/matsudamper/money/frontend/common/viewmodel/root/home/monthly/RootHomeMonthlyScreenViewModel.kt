package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.monthly.RootHomeMonthlyScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenViewModel

public class RootHomeMonthlyScreenViewModel(
    private val coroutineScope: CoroutineScope,
    argument: RootHomeScreenStructure.Monthly,
    loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            argument = argument,
        ),
    )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val tabViewModel = RootHomeTabScreenViewModel(
        coroutineScope = coroutineScope,
        loginCheckUseCase = loginCheckUseCase,
    ).also { viewModel ->
        coroutineScope.launch {
            viewModel.viewModelEventHandler.collect(
                object : RootHomeTabScreenViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        coroutineScope.launch { eventSender.send { it.navigate(screen) } }
                    }
                },
            )
        }
    }
    public val uiStateFlow: StateFlow<RootHomeMonthlyScreenUiState> = MutableStateFlow(
        RootHomeMonthlyScreenUiState(
            loadingState = RootHomeMonthlyScreenUiState.LoadingState.Loading,
            rootHomeTabUiState = tabViewModel.uiStateFlow.value,
            event = object : RootHomeMonthlyScreenUiState.Event {
                override suspend fun onViewInitialized() {

                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            tabViewModel.uiStateFlow.collectLatest { rootHomeTabUiState ->
                uiStateFlow.value = uiStateFlow.value.copy(
                    rootHomeTabUiState = rootHomeTabUiState,
                )
            }
        }
    }.asStateFlow()

    public fun updateStructure(current: RootHomeScreenStructure.Monthly) {
        viewModelStateFlow.value = viewModelStateFlow.value.copy(argument = current)
        tabViewModel.updateScreenStructure(current)
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)

    }

    private data class ViewModelState(
        val argument: RootHomeScreenStructure.Monthly,
    )
}