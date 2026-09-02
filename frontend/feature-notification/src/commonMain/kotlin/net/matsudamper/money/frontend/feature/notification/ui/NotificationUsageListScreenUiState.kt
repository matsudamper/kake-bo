package net.matsudamper.money.frontend.feature.notification.ui

import androidx.compose.runtime.Immutable
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener

public data class NotificationUsageListScreenUiState(
    val title: String,
    val itemsState: ItemsState,
    val filters: ImmutableList<Filter>,
    val searchListener: SearchListener?,
    val accessSection: AccessSection?,
    val topBarActions: ImmutableList<TopBarAction>,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    public sealed interface ItemsState {
        public data object Loading : ItemsState

        public data class Loaded(
            val items: ImmutableList<Item>,
            val emptyText: String,
        ) : ItemsState
    }

    @Immutable
    public interface SearchListener {
        public fun onSearchQueryChange(query: String)
    }

    public data class AccessSection(
        val title: String,
        val description: String,
        val buttonLabel: String?,
        val listener: AccessSectionListener?,
    )

    @Immutable
    public interface AccessSectionListener {
        public fun onClickButton()
    }

    public data class Item(
        val title: String,
        val receivedAt: String,
        val statusLabel: String,
        val description: String,
        val listener: ItemListener?,
    )

    @Immutable
    public interface ItemListener {
        public fun onClick()

        public fun onClickCopyJson()
    }

    public data class Filter(
        val label: String,
        val selected: Boolean,
        val listener: FilterListener,
    )

    @Immutable
    public interface FilterListener {
        public fun onClick()
    }

    public data class TopBarAction(
        val label: String,
        val listener: TopBarActionListener,
    )

    @Immutable
    public interface TopBarActionListener {
        public fun onClick()
    }
}
