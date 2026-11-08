package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.layout.TextField
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Composable
internal fun AdminLoginScreen(uiState: AdminLoginScreenUiState) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
        ) {
            Column(
                Modifier
                    .padding(16.dp),
            ) {
                val textFieldTextStyle = MaterialTheme.typography.bodyMedium
                    .merge(
                        TextStyle(
                            fontFamily = rememberCustomFontFamily(),
                        ),
                    )
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "管理画面",
                    fontFamily = rememberCustomFontFamily(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(24.dp))
                when (val content = uiState.content) {
                    is AdminLoginScreenUiState.Content.SelectAccount -> {
                        SelectAccountContent(
                            content = content,
                            listener = uiState.listener,
                        )
                    }

                    is AdminLoginScreenUiState.Content.AdminPassword -> {
                        AdminPasswordContent(
                            content = content,
                            listener = uiState.listener,
                            textFieldTextStyle = textFieldTextStyle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectAccountContent(
    content: AdminLoginScreenUiState.Content.SelectAccount,
    listener: AdminLoginScreenUiState.Listener,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { listener.onClickUserAccount() },
        ) {
            Text(
                text = "ユーザーアカウントでログイン",
                fontFamily = rememberCustomFontFamily(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = content.adminAccountButtonEnabled,
            onClick = { listener.onClickAdminAccount() },
        ) {
            Text(
                text = "管理アカウントでログイン",
                fontFamily = rememberCustomFontFamily(),
            )
        }
    }
}

@Composable
private fun AdminPasswordContent(
    content: AdminLoginScreenUiState.Content.AdminPassword,
    listener: AdminLoginScreenUiState.Listener,
    textFieldTextStyle: TextStyle,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { listener.onPasswordChanged(it) },
            text = content.password.text,
            textStyle = textFieldTextStyle,
            label = "Password",
            maxLines = 1,
            type = TextFieldType.Password,
            autocomplete = "current-password",
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { listener.onClickBackToSelectAccount() }) {
                Text(
                    text = "戻る",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { listener.onClickPasswordLogin() }) {
                Text(
                    text = "ログイン",
                    fontFamily = rememberCustomFontFamily(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun AdminLoginScreenAdminDisabledPreview() {
    AppRoot(isDarkTheme = true) {
        AdminLoginScreen(
            uiState = createPreviewUiState(
                content = AdminLoginScreenUiState.Content.SelectAccount(adminAccountButtonEnabled = false),
            ),
        )
    }
}

@Preview
@Composable
private fun AdminLoginScreenAdminEnabledPreview() {
    AppRoot(isDarkTheme = true) {
        AdminLoginScreen(
            uiState = createPreviewUiState(
                content = AdminLoginScreenUiState.Content.SelectAccount(adminAccountButtonEnabled = true),
            ),
        )
    }
}

@Preview
@Composable
private fun AdminLoginScreenAdminPasswordPreview() {
    AppRoot(isDarkTheme = true) {
        AdminLoginScreen(
            uiState = createPreviewUiState(
                content = AdminLoginScreenUiState.Content.AdminPassword(password = TextFieldValue()),
            ),
        )
    }
}

private fun createPreviewUiState(content: AdminLoginScreenUiState.Content): AdminLoginScreenUiState {
    return AdminLoginScreenUiState(
        content = content,
        listener = object : AdminLoginScreenUiState.Listener {
            override fun onClickUserAccount() = Unit

            override fun onClickAdminAccount() = Unit

            override fun onPasswordChanged(text: String) = Unit

            override fun onClickPasswordLogin() = Unit

            override fun onClickBackToSelectAccount() = Unit
        },
    )
}
