package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener

public data class RootHomeMonthlySubCategoryScreenUiState(
    val loadingState: LoadingState,
    val headerTitle: String,
    val event: Event,
    val scaffoldListener: RootScreenScaffoldListener,
    val currentSortType: SortType,
    val sortOrder: SortOrder,
) {
    public enum class SortType {
        Date,
        Amount,
    }

    public enum class SortOrder {
        Ascending,
        Descending,
    }

    public data class Item(
        val title: String,
        val amount: String,
        val category: String,
        val date: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClick()
        }
    }

    public sealed interface LoadingState {
        public data class Loaded(
            val items: List<Item>,
            val hasMoreItem: Boolean,
            val event: LoadedEvent,
        ) : LoadingState

        public data object Loading : LoadingState

        public data object Error : LoadingState
    }

    @Immutable
    public interface LoadedEvent {
        public fun loadMore()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
        public fun onSortTypeChanged(sortType: SortType)
        public fun onSortOrderChanged(order: SortOrder)
    }
}
