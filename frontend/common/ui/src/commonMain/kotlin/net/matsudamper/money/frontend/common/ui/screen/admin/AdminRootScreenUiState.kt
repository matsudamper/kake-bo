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

        public fun onClickUserSearch()
    }
}

public data class AdminAddUserUiState(
    val onChangeUserName: (String) -> Unit,
    val onChangePassword: (String) -> Unit,
    val onClickAddButton: () -> Unit,
)

public data class AdminUserSearchUiState(
    val searchQuery: String,
    val searchResults: List<String>,
    val selectedUserName: String?,
    val replacePasswordDialogState: ReplacePasswordDialogState?,
    val listener: Listener,
) {
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
    }
}
