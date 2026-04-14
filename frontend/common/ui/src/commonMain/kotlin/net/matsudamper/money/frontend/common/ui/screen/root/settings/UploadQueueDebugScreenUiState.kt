package net.matsudamper.money.frontend.common.ui.screen.root.settings

public data class UploadQueueDebugScreenUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val items: List<Item>,
            val isLoadingMore: Boolean,
            val isLast: Boolean,
        ) : LoadingState
    }

    public data class Item(
        val id: String,
        val moneyUsageId: Int,
        val status: Status,
        val errorMessage: String?,
        val createdAt: Long,
        val workManagerId: String?,
    )

    public sealed interface Status {
        public data object Pending : Status

        public data object Uploading : Status

        public data class Failed(val message: String?) : Status
    }

    public interface Event {
        public fun onLoadMore()

        public fun onRetry()
    }
}
