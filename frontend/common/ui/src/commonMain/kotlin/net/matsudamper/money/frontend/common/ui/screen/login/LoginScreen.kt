package net.matsudamper.money.frontend.common.ui.screen.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import net.matsudamper.money.frontend.common.ui.layout.DummyTextField
import net.matsudamper.money.frontend.common.ui.layout.DummyTextFieldDefaults
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
public fun LoginScreen(
    uiState: LoginScreenUiState,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    uiState.textInputDialog?.also { dialog ->
        HtmlFullScreenTextInput(
            title = dialog.title,
            name = dialog.name,
            default = dialog.text,
            inputType = dialog.inputType,
            onComplete = {
                dialog.onComplete(it)
            },
            isMultiline = false,
            canceled = { dialog.onCancel() },
        )
    }
    Surface(
        modifier = modifier.focusRequester(focusRequester),
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
                        onClickUserNameTextField = { uiState.listener.onClickUserNameTextField() },
                        textFieldTextStyle = textFieldTextStyle,
                    )
                } else {
                    PasswordInput(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        text = uiState.password.text,
                        onClickLogin = { uiState.listener.onClickLogin() },
                        textFieldTextStyle = textFieldTextStyle,
                        onClickSecurityKeyLogin = { uiState.listener.onClickSecurityKeyLogin() },
                        onClickDeviceKeyLogin = { uiState.listener.onClickDeviceKeyLogin() },
                        onClickPasswordTextField = { uiState.listener.onClickPasswordTextField() },
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
    onClickUserNameTextField: () -> Unit,
    text: String,
    textFieldTextStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    LoginContainer(
        modifier = modifier,
        onClickBack = null,
        footer = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { onNextClick() },
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        },
        content = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onClickUserNameTextField() },
            ) {
                DummyTextField(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    textStyle = textFieldTextStyle,
                    label = {
                        Text(
                            text = "User Name",
                            style = textFieldTextStyle,
                        )
                    },
                    maxLines = 1,
                    colors = DummyTextFieldDefaults.colors(),
                )
            }
        },
    )
}

@Composable
private fun PasswordInput(
    text: String,
    textFieldTextStyle: TextStyle,
    onClickLogin: () -> Unit,
    onClickSecurityKeyLogin: () -> Unit,
    onClickDeviceKeyLogin: () -> Unit,
    onClickPasswordTextField: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoginContainer(
        modifier = modifier,
        onClickBack = onClickBack,
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
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { onClickDeviceKeyLogin() },
                ) {
                    Text(
                        text = "スマホのロックでログイン",
                        fontFamily = rememberCustomFontFamily(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        content = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onClickPasswordTextField() },
            ) {
                DummyTextField(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    textStyle = textFieldTextStyle,
                    label = {
                        Text(
                            text = "password",
                            style = textFieldTextStyle,
                        )
                    },
                    maxLines = 1,
                    trailingIcon = {
                        IconButton(
                            modifier = Modifier,
                            onClick = { onClickLogin() },
                        ) {
                            Icon(
                                modifier = Modifier,
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "login",
                                tint = LocalContentColor.current,
                            )
                        }
                    },
                    colors = DummyTextFieldDefaults.colors(),
                )
            }
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
                        imageVector = Icons.Default.ArrowBack,
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
