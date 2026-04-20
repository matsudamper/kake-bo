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
    val searchResults: List<String>,
    val selectedUserName: String?,
    val resetPasswordDialogState: ResetPasswordDialogState?,
    val listener: Listener,
) {
    public data class ResetPasswordDialogState(
        val userName: String,
        val resultMessage: String?,
    )

    @Immutable
    public interface Listener {
        public fun onSearchQueryChanged(query: String)

        public fun onClickSearch()

        public fun onClickUser(userName: String)

        public fun onDismissUserMenu()

        public fun onClickResetPassword()

        public fun onPasswordChanged(password: String)

        public fun onClickSubmitResetPassword()

        public fun onDismissResetPasswordDialog()
    }
}
