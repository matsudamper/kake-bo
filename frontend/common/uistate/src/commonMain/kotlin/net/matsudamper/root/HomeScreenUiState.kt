package net.matsudamper.root

public data class HomeScreenUiState(
    val screenState: ScreenState,
    val event: Event,
) {
    public sealed interface ScreenState {
        public object Loading: ScreenState
        public data class Loaded(
            val notImportMailCount: Int?,
        ): ScreenState
    }
    public interface Event {
        public fun onResume()
    }
}
