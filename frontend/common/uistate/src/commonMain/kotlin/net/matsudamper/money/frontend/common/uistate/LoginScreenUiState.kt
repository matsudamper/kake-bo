package net.matsudamper.money.frontend.common.uistate

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

public data class LoginScreenUiState(
    val userName: TextFieldValue,
    val password: TextFieldValue,
    val listener: Listener,
) {
    @Immutable
    public interface Listener {
        public fun onPasswordChange(value: TextFieldValue)
        public fun onUserNameChange(value: TextFieldValue)
        public fun onClickLogin()
        public fun onClickNavigateAdmin()
    }
}
