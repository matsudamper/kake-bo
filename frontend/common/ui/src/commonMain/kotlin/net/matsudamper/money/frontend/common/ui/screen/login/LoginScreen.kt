package net.matsudamper.money.frontend.common.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.layout.TextField
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
public fun LoginScreen(
    uiState: LoginScreenUiState,
    modifier: Modifier = Modifier,
    windowInsets: PaddingValues,
) {
    val focusRequester = remember { FocusRequester() }
    Surface(
        modifier = modifier
            .padding(windowInsets)
            .focusRequester(focusRequester),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
            ) {
                val textFieldTextStyle = MaterialTheme.typography.bodyMedium
                    .merge(
                        TextStyle(
                            fontFamily = rememberCustomFontFamily(),
                        ),
                    )
                var userIdInput by remember { mutableStateOf(true) }
                if (userIdInput) {
                    UserIdInput(
                        modifier = Modifier.align(Alignment.Center),
                        text = uiState.userName.text,
                        onNextClick = { userIdInput = false },
                        textFieldTextStyle = textFieldTextStyle,
                        onValueChange = { uiState.listener.onUserIdChanged(it) },
                        onClickSecurityKeyLogin = { uiState.listener.onClickSecurityKeyLogin() },
                        onClickDeviceKeyLogin = { uiState.listener.onClickDeviceKeyLogin() },
                    )
                } else {
                    PasswordInput(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onValueChange = { uiState.listener.onPasswordChanged(it) },
                        text = uiState.password.text,
                        onClickLogin = { uiState.listener.onClickLogin() },
                        textFieldTextStyle = textFieldTextStyle,
                        onClickBack = { userIdInput = true },
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(12.dp),
            ) {
                OutlinedButton(
                    onClick = { uiState.listener.onClickNavigateAdmin() },
                ) {
                    Text(text = "Admin Login Page")
                }
            }
        }
    }
}

@Composable
private fun UserIdInput(
    onNextClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onClickSecurityKeyLogin: () -> Unit,
    onClickDeviceKeyLogin: () -> Unit,
    text: String,
    textFieldTextStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    LoginContainer(
        modifier = modifier,
        onClickBack = null,
        footer = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { onClickSecurityKeyLogin() },
                ) {
                    Text(
                        text = "セキュリティキーでログイン",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { onClickDeviceKeyLogin() },
                ) {
                    Text(
                        text = "デバイスのロックでログイン",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = onValueChange,
                    text = text,
                    textStyle = textFieldTextStyle,
                    label = "User Name",
                    maxLines = 1,
                )
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { onNextClick() },
                ) {
                    Text(
                        text = "Next",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        },
    )
}

@Composable
private fun PasswordInput(
    text: String,
    onValueChange: (String) -> Unit,
    textFieldTextStyle: TextStyle,
    onClickLogin: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoginContainer(
        modifier = modifier,
        onClickBack = onClickBack,
        footer = {
        },
        content = {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onValueChange,
                text = text,
                textStyle = textFieldTextStyle,
                label = "password",
                maxLines = 1,
                trailingIcon = {
                    IconButton(
                        onClick = { onClickLogin() },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "login",
                            tint = LocalContentColor.current,
                        )
                    }
                },
            )
        },
    )
}

@Composable
private fun LoginContainer(
    onClickBack: (() -> Unit)?,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClickBack != null) {
                IconButton(
                    onClick = { onClickBack() },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "back",
                        tint = LocalContentColor.current,
                    )
                }
            }
            Text(
                modifier = Modifier.weight(1f),
                fontFamily = rememberCustomFontFamily(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                text = "ログイン",
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
        Spacer(modifier = Modifier.height(16.dp))
        footer()
    }
}
