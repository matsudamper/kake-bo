package net.matsudamper.money.frontend.common.ui.screen.root.preset

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener

public data class PresetListScreenUiState(
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val loadingState: LoadingState,
    val showNameInput: Boolean,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState
        public data class Loaded(
            val items: ImmutableList<PresetItem>,
        ) : LoadingState
    }

    public data class PresetItem(
        val id: String,
        val name: String,
        val subCategoryName: String?,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClick()

            public fun onClickEdit()

            public fun onClickDelete()
        }
    }

    @Immutable
    public interface Event {
        public fun onResume()

        public fun onRefresh()

        public fun onClickAddButton()

        public fun onNameInputCompleted(name: String)

        public fun onDismissNameInput()
    }
}
