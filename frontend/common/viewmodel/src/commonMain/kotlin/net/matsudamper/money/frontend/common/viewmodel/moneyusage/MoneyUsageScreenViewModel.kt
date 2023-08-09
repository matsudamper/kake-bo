package net.matsudamper.money.frontend.common.viewmodel.moneyusage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.matsudamper.money.frontend.common.ui.screen.moneyusage.MoneyUsageScreenUiState

public class MoneyUsageScreenViewModel(
    coroutineScope: CoroutineScope
) {
    public val uiStateFlow: StateFlow<MoneyUsageScreenUiState> = MutableStateFlow(
        MoneyUsageScreenUiState(
            event = object : MoneyUsageScreenUiState.Event {
            },
            loadingState = MoneyUsageScreenUiState.LoadingState.Loading,
        )
    )
}