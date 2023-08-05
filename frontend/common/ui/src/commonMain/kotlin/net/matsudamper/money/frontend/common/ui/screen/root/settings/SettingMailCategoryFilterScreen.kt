package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

public data class SettingMailCategoryFilterScreenUiState(
    public val event: Event,
) {
    @Immutable
    public interface Event
}

@Composable
public fun SettingMailCategoryFilterScreen(
    modifier: Modifier = Modifier,
    uiState: SettingMailCategoryFilterScreenUiState,
) {
}
