package net.matsudamper.money.frontend.common.ui.screen.admin.user

import androidx.compose.runtime.Immutable

public data class AdminUserSearchUiState(
    val searchQuery: String,
    val users: List<User>,
    val hasMore: Boolean,
    val replacePasswordDialogState: ReplacePasswordDialogState?,
    val userOperationDialogUiState: UserOperationDialogState?,
    val listener: Listener,
) {
    public data class User(
        val displayId: String,
        val name: String,
        val listener: Listener,
    ) {
        @Immutable
        public interface Listener {
            public fun onClick()
            public fun dismiss()
        }
    }

    public data class ReplacePasswordDialogState(
        val userName: String,
        val resultMessage: String?,
        val listener: Listener,
    ) {
        @Immutable
        public interface Listener {
            public fun passwordInputDone(password: String)
            public fun dismiss()
        }
    }

    @Immutable
    public interface Listener {
        public fun onSearchQueryChanged(query: String)

        public fun onClickSearch()

        public fun loadMore()
    }
}
