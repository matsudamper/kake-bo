package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.ui.text.input.TextFieldValue

public data class AdminAddUserUiState(
    val userName: TextFieldValue,
    val password: TextFieldValue,
    val onChangeUserName: (String) -> Unit,
    val onChangePassword: (String) -> Unit,
    val onClickAddButton: () -> Unit,
)
