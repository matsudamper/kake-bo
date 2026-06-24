package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

internal data class AdminAddUserUiState(
    val userName: TextFieldValue,
    val password: TextFieldValue,
    val listener: Listener,
) {
    @Immutable
    interface Listener {
        fun onChangeUserName(value: String)
        fun onChangePassword(value: String)
        fun onClickAddButton()
    }
}
