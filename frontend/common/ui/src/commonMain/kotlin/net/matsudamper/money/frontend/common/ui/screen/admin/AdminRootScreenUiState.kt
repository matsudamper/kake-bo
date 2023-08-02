package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.ui.text.input.TextFieldValue

public data class AdminLoginScreenUiState(
    val onChangePassword: (TextFieldValue) -> Unit,
    val onClickLogin: () -> Unit,
)

public data class AdminRootScreenUiState(
    val listener: Listener,
) {
    public interface Listener {
        public fun onClickAddUser()
    }
}

public data class AdminAddUserUiState(
    val onChangeUserName: (String) -> Unit,
    val onChangePassword: (String) -> Unit,
    val onClickAddButton: () -> Unit,
)
