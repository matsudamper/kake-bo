package net.matsudamper.money.frontend.feature.admin.ui.user

import androidx.compose.runtime.Immutable

internal data class AdminUserSearchUiState(
    val searchQuery: String,
    val users: List<User>,
    val hasMore: Boolean,
    val replacePasswordDialogState: ReplacePasswordDialogState?,
    val userOperationDialogUiState: UserOperationDialogState?,
    val listener: Listener,
) {
    internal data class User(
        val displayId: String,
        val name: String,
        val listener: Listener,
    ) {
        @Immutable
        interface Listener {
            fun onClick()
            fun dismiss()
        }
    }

    internal data class ReplacePasswordDialogState(
        val userName: String,
        val resultMessage: String?,
        val listener: Listener,
    ) {
        @Immutable
        interface Listener {
            fun passwordInputDone(password: String)
            fun dismiss()
        }
    }

    @Immutable
    interface Listener {
        fun onSearchQueryChanged(query: String)

        fun onClickSearch()

        fun loadMore()
    }
}
