package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

internal data class AdminLoginScreenUiState(
    val content: Content,
    val listener: Listener,
) {
    sealed interface Content {
        data class SelectAccount(
            val adminAccountButtonEnabled: Boolean,
        ) : Content

        data class UserAccount(
            val userName: TextFieldValue,
            val password: TextFieldValue,
        ) : Content

        data class AdminAccount(
            val password: TextFieldValue,
        ) : Content
    }

    @Immutable
    interface Listener {
        fun onClickUserAccount()

        fun onClickAdminAccount()

        fun onUserNameChanged(text: String)

        fun onUserPasswordChanged(text: String)

        fun onClickUserLogin()

        fun onClickUserSecurityKeyLogin()

        fun onClickUserDeviceKeyLogin()

        fun onAdminPasswordChanged(text: String)

        fun onClickAdminLogin()

        fun onClickBackToSelectAccount()
    }
}

internal data class AdminRootScreenUiState(
    val listener: Listener,
) {
    interface Listener {
        fun onClickAddUser()

        fun onClickUnlinkedImages()

        fun onClickUserSearch()

        fun onClickThumbnailBackfill()

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
