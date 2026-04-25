package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.ui.text.input.TextFieldValue

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
