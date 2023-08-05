package net.matsudamper.money.frontend.common.viewmodel.root.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingMailCategoryFilterScreenUiState

public class SettingMailCategoryFilterViewModel(
    private val coroutineScope: CoroutineScope
) {
    public val uiStateFlow: StateFlow<SettingMailCategoryFilterScreenUiState> = MutableStateFlow(
        SettingMailCategoryFilterScreenUiState(
            event = object : SettingMailCategoryFilterScreenUiState.Event {

            }
        )
    )
}