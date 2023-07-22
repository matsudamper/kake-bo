package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.root.HomeScreenUiState

public class HomeViewModel(
    private val coroutineScope: CoroutineScope,
    private val homeGraphqlApi: HomeGraphqlApi,
    private val loginCheckUseCase: LoginCheckUseCase,
) {
    private val event = object : HomeScreenUiState.Event {
        override fun onResume() {
            coroutineScope
        }
    }

    public val uiStateFlow: StateFlow<HomeScreenUiState> = MutableStateFlow(
        HomeScreenUiState(
            screenState = HomeScreenUiState.ScreenState.Loading,
            event = event,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            homeGraphqlApi.getHomeScreen().collect {
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        screenState = run screenState@{
                            HomeScreenUiState.ScreenState.Loaded(
                                notImportMailCount = it.data?.user?.userMailAttributes?.mailCount,
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
}
