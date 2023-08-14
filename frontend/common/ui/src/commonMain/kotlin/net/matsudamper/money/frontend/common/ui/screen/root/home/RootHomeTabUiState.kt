package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.ktor.http.ContentType
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.layout.graph.PolygonalLineGraphItemUiState

public data class RootHomeTabUiState(
    val screenState: ScreenState,
    val event: Event,
) {
    public sealed interface ScreenState {
        public object Loading : ScreenState
        public object Error : ScreenState
        public data class Loaded(
            val displayType: DisplayType,
        ) : ScreenState
    }

    public sealed interface DisplayType {
        public data class Between(
            val between: String,
            val totals: ImmutableList<PolygonalLineGraphItemUiState>,
            val totalBar: BarGraphUiState,
            val totalBarColorTextMapping: ImmutableList<ColorText>,
            val rangeText: String,
            val event: BetweenEvent,
        ) : DisplayType
    }

    public data class ColorText(
        val color: Color,
        val text: String,
        val onClick: () -> Unit,
    )

    @Immutable
    public interface BetweenEvent {
        public fun onClickNextMonth()
        public fun onClickPreviousMonth()
        public fun onClickRange(range: Int)
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
    }
}
