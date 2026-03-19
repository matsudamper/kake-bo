package net.matsudamper.money.frontend.common.ui.screen.root.preset

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.NumberInputValue

public data class PresetDetailScreenUiState(
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val loadingState: LoadingState,
    val showNameChangeDialog: FullScreenInputDialog?,
    val numberInputDialog: NumberInputDialog?,
    val showDescriptionChangeDialog: FullScreenInputDialog?,
    val categorySelectDialog: CategorySelectDialogUiState?,
    val event: Event,
) {
    public data class NumberInputDialog(
        val value: NumberInputValue,
        val onChangeValue: (NumberInputValue) -> Unit,
        val dismissRequest: () -> Unit,
    )

    public data class FullScreenInputDialog(
        val defaultText: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onDismiss()

            public fun onCompleted(text: String)
        }
    }

    @Immutable
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState

        public data class Loaded(
            val presetName: String,
            val subCategoryName: String,
            val amount: Int?,
            val description: String?,
        ) : LoadingState
    }

    @Immutable
    public interface Event {
        public fun onResume()
        public fun onClickRetry()
        public fun onRefresh()

        public fun onClickPresetNameChange()

        public fun onClickSubCategoryChange()

        public fun onClickAmountChange()

        public fun onClickDescriptionChange()

        public fun onClickBack()
    }
}