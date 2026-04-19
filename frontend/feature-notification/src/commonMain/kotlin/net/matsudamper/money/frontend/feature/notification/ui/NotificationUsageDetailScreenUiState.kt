package net.matsudamper.money.frontend.feature.notification.ui

import androidx.compose.runtime.Immutable

public data class NotificationUsageDetailScreenUiState(
    val loadingState: LoadingState,
    val deleteConfirmDialog: DeleteConfirmDialog?,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object NotFound : LoadingState

        public data class Loaded(
            val notification: Notification,
            val filter: Filter,
            val draft: Draft?,
            val linkedUsage: LinkedUsageState,
            val metadataDialog: MetadataDialog?,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class DeleteConfirmDialog(
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit,
    )

    public data class Notification(
        val packageName: String,
        val status: String,
        val postedAt: String,
        val receivedAt: String,
        val text: String,
        val metadata: String,
        val event: NotificationEvent,
    )

    @Immutable
    public interface NotificationEvent {
        public fun onClickMetadata()
    }

    public data class MetadataDialog(
        val text: String,
        val event: MetadataDialogEvent,
    )

    @Immutable
    public interface MetadataDialogEvent {
        public fun onDismiss()
    }

    public sealed interface Filter {
        public data object NotMatched : Filter

        public data class Matched(
            val title: String,
            val description: String,
        ) : Filter
    }

    public data class Draft(
        val title: String,
        val description: String,
        val amount: String,
        val dateTime: String,
        val subCategory: String,
    )

    public sealed interface LinkedUsageState {
        public data object None : LinkedUsageState

        public data object Loading : LinkedUsageState

        public data object MissingUsageId : LinkedUsageState

        public data object Error : LinkedUsageState

        public data class Loaded(
            val usage: LinkedUsage,
        ) : LinkedUsageState
    }

    public data class LinkedUsage(
        val title: String,
        val category: String,
        val amount: String,
        val dateTime: String,
        val event: LinkedUsageEvent,
    )

    @Immutable
    public interface LinkedUsageEvent {
        public fun onClick()
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickRegister()
    }

    @Immutable
    public interface Event {
        public fun onClickBack()

        public fun onClickTitle()

        public fun onClickDelete()
    }
}
