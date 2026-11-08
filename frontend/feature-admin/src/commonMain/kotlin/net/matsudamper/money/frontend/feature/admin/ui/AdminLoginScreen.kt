package net.matsudamper.money.frontend.feature.admin.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
                AccountTypeOption(
                    text = "ユーザーアカウントでログイン",
                    selected = uiState.selectedAccountType == AdminLoginScreenUiState.AccountType.User,
                    enabled = true,
                    onClick = { uiState.listener.onAccountTypeSelected(AdminLoginScreenUiState.AccountType.User) },
                )
                AccountTypeOption(
                    text = "管理アカウントでログイン",
                    selected = uiState.selectedAccountType == AdminLoginScreenUiState.AccountType.Admin,
                    enabled = uiState.adminAccountOptionEnabled,
                    onClick = { uiState.listener.onAccountTypeSelected(AdminLoginScreenUiState.AccountType.Admin) },
                )
                if (uiState.passwordInputVisible) {
                    Spacer(Modifier.height(16.dp))
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
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { uiState.listener.onClickLogin() },
                ) {
                    Text(
                        text = "ログイン",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountTypeOption(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            enabled = enabled,
            onClick = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontFamily = rememberCustomFontFamily(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
    }
}

@Preview
@Composable
private fun AdminLoginScreenAdminDisabledPreview() {
    AppRoot(isDarkTheme = true) {
        AdminLoginScreen(
            uiState = createPreviewUiState(
                selectedAccountType = AdminLoginScreenUiState.AccountType.User,
                adminAccountOptionEnabled = false,
            ),
        )
    }
}

@Preview
@Composable
private fun AdminLoginScreenAdminSelectedPreview() {
    AppRoot(isDarkTheme = true) {
        AdminLoginScreen(
            uiState = createPreviewUiState(
                selectedAccountType = AdminLoginScreenUiState.AccountType.Admin,
                adminAccountOptionEnabled = true,
            ),
        )
    }
}

private fun createPreviewUiState(
    selectedAccountType: AdminLoginScreenUiState.AccountType,
    adminAccountOptionEnabled: Boolean,
): AdminLoginScreenUiState {
    return AdminLoginScreenUiState(
        selectedAccountType = selectedAccountType,
        adminAccountOptionEnabled = adminAccountOptionEnabled,
        passwordInputVisible = selectedAccountType == AdminLoginScreenUiState.AccountType.Admin,
        password = TextFieldValue(),
        listener = object : AdminLoginScreenUiState.Listener {
            override fun onAccountTypeSelected(accountType: AdminLoginScreenUiState.AccountType) = Unit

            override fun onPasswordChanged(text: String) = Unit

            override fun onClickLogin() = Unit
        },
    )
}
