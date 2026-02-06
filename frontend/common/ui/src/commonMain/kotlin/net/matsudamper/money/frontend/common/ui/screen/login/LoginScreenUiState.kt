package net.matsudamper.money.frontend.common.ui.screen.login

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

public data class LoginScreenUiState(
    val userName: TextFieldValue,
    val password: TextFieldValue,
    val serverHost: ServerHostUiState?,
    val listener: Listener,
) {
    public data class ServerHostUiState(
        val selectedHost: String,
        val hosts: List<String>,
        val customHostDialogText: String?,
    )

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

        public fun onSelectServerHost(host: String)
        public fun onClickAddCustomHost()
        public fun onCustomHostTextChanged(text: String)
        public fun onConfirmCustomHost()
        public fun onDismissCustomHostDialog()
    }
}
