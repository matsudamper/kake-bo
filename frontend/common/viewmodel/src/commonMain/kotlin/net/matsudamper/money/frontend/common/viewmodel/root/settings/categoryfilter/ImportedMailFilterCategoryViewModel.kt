package net.matsudamper.money.frontend.common.viewmodel.root.settings.categoryfilter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImportedMailFilterCategoryScreenUiState

public class ImportedMailFilterCategoryViewModel(
    private val coroutineScope: CoroutineScope,
) {
    public val uiStateFlow: StateFlow<ImportedMailFilterCategoryScreenUiState> = MutableStateFlow(
        ImportedMailFilterCategoryScreenUiState(
            textInput = null,
            event = object : ImportedMailFilterCategoryScreenUiState.Event {
                override fun onViewInitialized() {
                    // TODO
                }
            }
        )
    ).asStateFlow()
}