package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

internal data class AdminLoginScreenUiState(
    val password: TextFieldValue,
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        fun onPasswordChanged(text: String)

        fun onClickLogin()
    }
}

internal data class AdminRootScreenUiState(
    val listener: Listener,
) {
    interface Listener {
        fun onClickAddUser()

        fun onClickUnlinkedImages()

        fun onClickUserSearch()

        fun onClickLogout()
    }
}

internal data class AdminUnlinkedImagesScreenUiState(
    val loadingState: LoadingState,
    val deleteDialog: DeleteDialog?,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState
        data class Loaded(
            val items: List<Item>,
            val hasMore: Boolean,
            val isLoadingMore: Boolean,
            val totalCount: Int?,
            val selectedCount: Int,
            val isAllSelected: Boolean,
            val isSelectingAll: Boolean,
            val isDeleting: Boolean,
        ) : LoadingState
    }

    internal data class Item(
        val id: String,
        val imageUrl: String,
        val userId: String,
        val userName: String,
        val isSelected: Boolean,
        val event: Event,
    ) {
        @Immutable
        interface Event {
            fun onClickSelect()
        }
    }

    internal data class DeleteDialog(
        val selectedCount: Int,
        val errorMessage: String?,
        val isLoading: Boolean,
        val event: Event,
    ) {
        @Immutable
        interface Event {
            fun onConfirm()

            fun onCancel()

            fun onDismiss()
        }
    }

    @Immutable
    interface Event {
        fun onResume()

        fun onClickRetry()

        fun onClickLoadMore()

        fun onClickSelectAll()

        fun onClickDelete()
    }
}
