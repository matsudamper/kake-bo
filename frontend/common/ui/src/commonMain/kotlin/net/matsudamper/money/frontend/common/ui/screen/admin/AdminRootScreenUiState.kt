package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

public data class AdminLoginScreenUiState(
    val password: TextFieldValue,
    val listener: Listener,
) {
    @Immutable
    public interface Listener {
        public fun onPasswordChanged(text: String)

        public fun onClickLogin()
    }
}

public data class AdminRootScreenUiState(
    val listener: Listener,
) {
    public interface Listener {
        public fun onClickAddUser()

        public fun onClickUnlinkedImages()

        public fun onClickUserSearch()

        public fun onClickLogout()
    }
}

public data class AdminUnlinkedImagesScreenUiState(
    val loadingState: LoadingState,
    val deleteDialog: DeleteDialog?,
    val event: Event,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState
        public data object Error : LoadingState
        public data class Loaded(
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

    public data class Item(
        val id: String,
        val imageUrl: String,
        val userId: String,
        val userName: String,
        val isSelected: Boolean,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClickSelect()
        }
    }

    public data class DeleteDialog(
        val selectedCount: Int,
        val errorMessage: String?,
        val isLoading: Boolean,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onConfirm()

            public fun onCancel()

            public fun onDismiss()
        }
    }

    @Immutable
    public interface Event {
        public fun onResume()

        public fun onClickRetry()

        public fun onClickLoadMore()

        public fun onClickSelectAll()

        public fun onClickDelete()
    }
}

public data class AdminAddUserUiState(
    val userName: TextFieldValue,
    val password: TextFieldValue,
    val onChangeUserName: (String) -> Unit,
    val onChangePassword: (String) -> Unit,
    val onClickAddButton: () -> Unit,
) {
    public companion object {
        public const val PASSWORD_ALLOW_SYMBOLS_TEXT: String = "使用できる記号 !@#\$%^&*()_+-?<>,."
    }
}

public data class AdminUserSearchUiState(
    val searchQuery: String,
    val searchResults: List<SearchResult>,
    val hasMore: Boolean,
    val selectedUserName: String?,
    val replacePasswordDialogState: ReplacePasswordDialogState?,
    val listener: Listener,
) {
    public data class SearchResult(
        val name: String,
    )

    public data class ReplacePasswordDialogState(
        val userName: String,
        val password: String,
        val resultMessage: String?,
    )

    @Immutable
    public interface Listener {
        public fun onSearchQueryChanged(query: String)

        public fun onClickSearch()

        public fun onClickUser(userName: String)

        public fun onDismissUserMenu()

        public fun onClickReplacePassword()

        public fun onPasswordChanged(password: String)

        public fun onClickSubmitReplacePassword()

        public fun onDismissReplacePasswordDialog()

        public fun onClickLoadMore()
    }
}
