package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraphUiState

public data class RootHomeTabPeriodSubCategoryContentUiState(
    val loadingState: LoadingState,
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val periodUiState: PeriodUiState,
    val event: Event,
) {

    public data class PeriodUiState(
        val between: String,
        val rangeText: String,
    )

    @Immutable
    public sealed interface LoadingState {
        public data class Loaded(
            val graphItems: BarGraphUiState,
            val monthTotalItems: ImmutableList<MonthTotalItem>,
            val subCategoryName: String,
        ) : LoadingState

        public data object Loading : LoadingState

        public data object Error : LoadingState
    }

    public data class MonthTotalItem(
        val title: String,
        val amount: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClick()
        }
    }

    @Immutable
    public interface Event {
        public suspend fun onViewInitialized()

        public fun refresh()

        public fun onClickNextMonth()

        public fun onClickPreviousMonth()

        public fun onClickRange(range: Int)
    }
}
