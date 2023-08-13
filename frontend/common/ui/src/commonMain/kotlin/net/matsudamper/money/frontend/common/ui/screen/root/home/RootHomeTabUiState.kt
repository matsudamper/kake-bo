package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.base.ImmutableList

public data class RootHomeTabUiState(
    val screenState: ScreenState,
    val event: Event,
) {
    public sealed interface ScreenState {
        public object Loading : ScreenState
        public object Error : ScreenState
        public data class Loaded(
            val displayType: DisplayType,
            val event: LoadedEvent,
        ) : ScreenState
    }

    public sealed interface DisplayType {
        public data class Between(
            val between: String,
            val totals: ImmutableList<Total>,
            val event: BetweenEvent,
        ) : DisplayType
    }

    public data class Total(
        val year: Int,
        val month: Int,
        val amount: Long,
    )

    @Immutable
    public interface BetweenEvent {
        public fun onClickNextMonth()
        public fun onClickPreviousMonth()
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
