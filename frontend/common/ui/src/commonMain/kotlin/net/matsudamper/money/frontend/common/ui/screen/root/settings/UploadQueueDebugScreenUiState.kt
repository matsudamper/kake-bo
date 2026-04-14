package net.matsudamper.money.frontend.common.ui.screen.root.settings

public data class UploadQueueDebugScreenUiState(
    val items: List<Item>,
    val selectedStatusFilter: StatusFilter,
    val statusFilterExpanded: Boolean,
    val errorDialogItem: Item?,
    val event: Event,
) {
    public enum class StatusFilter {
        All,
        Pending,
        Uploading,
        Completed,
        Failed,
    }

    public data class Item(
        val id: String,
        val moneyUsageId: Int,
        val status: Status,
        val errorMessage: String?,
        val stackTrace: String?,
        val createdAt: Long,
        val workManagerId: String?,
    )

    public sealed interface Status {
        public data object Pending : Status

        public data object Uploading : Status

        public data object Completed : Status

        public data class Failed(val message: String?) : Status
    }

    public interface Event {
        public fun onClickStatusFilter()

        public fun onDismissStatusFilter()

        public fun onSelectStatusFilter(filter: StatusFilter)

        public fun onClickItem(item: Item)

        public fun onDismissErrorDialog()
    }
}
