package net.matsudamper.money.frontend.common.ui.screen.moneyusage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

public data class MoneyUsageScreenUiState(
    val event: Event,
) {
    @Immutable
    public interface Event
}
@Composable
public fun MoneyUsageScreen(
    modifier: Modifier = Modifier,
    uiState: MoneyUsageScreenUiState,
) {
}