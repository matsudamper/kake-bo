package net.matsudamper.money.frontend.common.ui.screen.root

import androidx.compose.runtime.Immutable

public data class HomeScreenUiState(
    val screenState: ScreenState,
    val event: Event,
) {
    public sealed interface ScreenState {
        public object Loading : ScreenState
        public data class Loaded(
            val notImportMailCount: Int?,
            val importedAndNotLinkedMailCount: Int?,
            val event: LoadedEvent,
        ) : ScreenState
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickMailImportButton()
        public fun onClickNotLinkedMailButton()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
    }
}
