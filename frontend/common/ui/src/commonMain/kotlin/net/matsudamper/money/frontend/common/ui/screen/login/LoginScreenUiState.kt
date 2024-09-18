package net.matsudamper.money.frontend.common.ui.screen.login

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

public data class LoginScreenUiState(
    val userName: TextFieldValue,
    val password: TextFieldValue,
    val listener: Listener,
) {
    public data class TextInputDialogUiState(
        val title: String,
        val name: String,
        val text: String,
        val inputType: String,
        val onComplete: (String) -> Unit,
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit,
    )

    @Immutable
    public interface Listener {
        public fun onClickLogin()

        public fun onClickNavigateAdmin()

        public fun onClickSecurityKeyLogin()

        public fun onClickDeviceKeyLogin()

        public fun onUserIdChanged(text: String)
        public fun onPasswordChanged(text: String)
    }
}
