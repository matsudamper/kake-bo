package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
public expect fun LoginCredentialsFields(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    usernameLabel: String,
    passwordLabel: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
