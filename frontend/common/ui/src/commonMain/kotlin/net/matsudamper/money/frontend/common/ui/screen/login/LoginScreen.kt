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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.layout.TextField
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
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
            Column(
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
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = rememberCustomFontFamily(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    text = "ログイン",
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { uiState.listener.onUserIdChanged(it) },
                    text = uiState.userName.text,
                    textStyle = textFieldTextStyle,
                    label = "User Name",
                    maxLines = 1,
                    autocomplete = "username",
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { uiState.listener.onPasswordChanged(it) },
                    text = uiState.password.text,
                    textStyle = textFieldTextStyle,
                    label = "Password",
                    maxLines = 1,
                    type = TextFieldType.Password,
                    autocomplete = "current-password",
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { uiState.listener.onClickLogin() },
                ) {
                    Text(
                        text = "ログイン",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = { uiState.listener.onClickSecurityKeyLogin() },
                    ) {
                        Text(
                            text = "セキュリティキーでログイン",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = { uiState.listener.onClickDeviceKeyLogin() },
                    ) {
                        Text(
                            text = "デバイスのロックでログイン",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
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

            val serverHostState = uiState.serverHost
            if (serverHostState != null) {
                ServerHostSection(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    serverHostState = serverHostState,
                    listener = uiState.listener,
                )
                val dialogText = serverHostState.customHostDialogText
                if (dialogText != null) {
                    CustomHostDialog(
                        text = dialogText,
                        onTextChanged = { uiState.listener.onCustomHostTextChanged(it) },
                        onConfirm = { uiState.listener.onConfirmCustomHost() },
                        onDismiss = { uiState.listener.onDismissCustomHostDialog() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerHostSection(
    serverHostState: LoginScreenUiState.ServerHostUiState,
    listener: LoginScreenUiState.Listener,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selectedHost = serverHostState.selectedHost
        if (selectedHost.isNotEmpty()) {
            Text(
                text = selectedHost,
                fontFamily = rememberCustomFontFamily(),
                style = MaterialTheme.typography.bodySmall,
            )
            IconButton(
                onClick = { listener.onClickChangeHost() },
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                )
            }
        } else {
            TextButton(
                onClick = { listener.onClickChangeHost() },
            ) {
                Text(
                    text = "ホストを設定",
                    fontFamily = rememberCustomFontFamily(),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CustomHostDialog(
    text: String,
    onTextChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ホスト設定",
                fontFamily = rememberCustomFontFamily(),
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                label = {
                    Text(
                        text = "ホスト名",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                placeholder = {
                    Text(
                        text = "example.com:443",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "OK",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "キャンセル",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        },
    )
}

@Composable
@Preview
private fun LoginScreenPreview() {
    AppRoot(isDarkTheme = false) {
        LoginScreen(
            uiState = LoginScreenUiState(
                userName = TextFieldValue(""),
                password = TextFieldValue(""),
                serverHost = null,
                listener = object : LoginScreenUiState.Listener {
                    override fun onClickLogin() {}
                    override fun onClickNavigateAdmin() {}
                    override fun onClickSecurityKeyLogin() {}
                    override fun onClickDeviceKeyLogin() {}
                    override fun onUserIdChanged(text: String) {}
                    override fun onPasswordChanged(text: String) {}
                    override fun onClickChangeHost() {}
                    override fun onCustomHostTextChanged(text: String) {}
                    override fun onConfirmCustomHost() {}
                    override fun onDismissCustomHostDialog() {}
                },
            ),
            modifier = Modifier.fillMaxSize(),
            windowInsets = PaddingValues(),
        )
    }
}
