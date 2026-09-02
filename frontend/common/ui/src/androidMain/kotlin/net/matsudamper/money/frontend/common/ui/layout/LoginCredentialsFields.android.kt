package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
public actual fun LoginCredentialsFields(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    usernameLabel: String,
    passwordLabel: String,
    textStyle: TextStyle,
    modifier: Modifier,
    enabled: Boolean,
) {
    Column(modifier = modifier) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onUsernameChange,
            text = username,
            textStyle = textStyle,
            label = usernameLabel,
            maxLines = 1,
            enabled = enabled,
            autocomplete = "username",
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onPasswordChange,
            text = password,
            textStyle = textStyle,
            label = passwordLabel,
            maxLines = 1,
            type = TextFieldType.Password,
            enabled = enabled,
            autocomplete = "current-password",
        )
    }
}
