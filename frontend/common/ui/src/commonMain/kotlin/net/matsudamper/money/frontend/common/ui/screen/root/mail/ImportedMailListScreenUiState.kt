package net.matsudamper.money.frontend.common.ui.screen.root.mail

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener

public data class ImportedMailListScreenUiState(
    val event: Event,
    val filters: Filters,
    val loadingState: LoadingState,
    val rootScreenScaffoldListener: RootScreenScaffoldListener,
    val operation: Flow<(Operation) -> Unit>,
) {
    public interface Operation {
        public fun scrollToTop()
    }

    public data class Filters(
        val link: Link,
    ) {
        public data class Link(
            val status: LinkStatus,
            val updateState: (LinkStatus) -> Unit,
        )

        public enum class LinkStatus {
            Undefined,
            Linked,
            NotLinked,
        }
    }

    @Immutable
    public sealed interface LoadingState {
        public data class Loaded(
            val listItems: ImmutableList<ListItem>,
            val showLastLoading: Boolean,
        ) : LoadingState

        public data object Loading : LoadingState
    }

    public data class ListItem(
        val mail: ImportedMail,
        val usages: ImmutableList<UsageItem>,
        val event: ListItemEvent,
    )

    public data class UsageItem(
        val title: String,
        val service: String,
        val description: String,
        val amount: String,
        val dateTime: String,
        val category: String,
    )

    public data class ImportedMail(
        val mailFrom: String,
        val mailSubject: String,
    )

    @Immutable
    public interface ListItemEvent {
        public fun onClickMailDetail()

        public fun onClick()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()

        public fun moreLoading()
    }
}
