package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.layout.html.text.input.HtmlTextInput
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserSearchScreen(
    modifier: Modifier = Modifier,
    uiState: AdminUserSearchUiState,
) {
    var searchQuery by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            Text(
                text = "ユーザー検索",
                fontFamily = rememberCustomFontFamily(),
            )
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HtmlTextInput(
                    modifier = Modifier.weight(1f).height(48.dp),
                    placeholder = "ユーザー名を入力",
                    onValueChange = {
                        searchQuery = it.text
                        uiState.listener.onSearchQueryChanged(it.text)
                    },
                    type = KeyboardType.Text,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { uiState.listener.onClickSearch() },
                ) {
                    Text(
                        text = "検索",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            LazyColumn {
                items(uiState.searchResults) { userName ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { uiState.listener.onClickUser(userName) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = userName,
                                fontFamily = rememberCustomFontFamily(),
                            )
                            DropdownMenu(
                                expanded = uiState.selectedUserName == userName,
                                onDismissRequest = { uiState.listener.onDismissUserMenu() },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "パスワードリセット",
                                            fontFamily = rememberCustomFontFamily(),
                                        )
                                    },
                                    onClick = { uiState.listener.onClickResetPassword() },
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    val dialogState = uiState.resetPasswordDialogState
    if (dialogState != null) {
        AlertDialog(
            onDismissRequest = { uiState.listener.onDismissResetPasswordDialog() },
            title = {
                Text(
                    text = "パスワードリセット: ${dialogState.userName}",
                    fontFamily = rememberCustomFontFamily(),
                )
            },
            text = {
                Column {
                    HtmlTextInput(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        placeholder = "新しいパスワード",
                        onValueChange = {
                            passwordText = it.text
                            uiState.listener.onPasswordChanged(it.text)
                        },
                        type = KeyboardType.Password,
                    )
                    Text(
                        "使用できる記号 !@#\$%^&*()_+-?<>,.",
                        fontFamily = rememberCustomFontFamily(),
                    )
                    if (dialogState.resultMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dialogState.resultMessage,
                            fontFamily = rememberCustomFontFamily(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { uiState.listener.onClickSubmitResetPassword() },
                ) {
                    Text(
                        text = "リセット",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { uiState.listener.onDismissResetPasswordDialog() },
                ) {
                    Text(
                        text = "キャンセル",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            },
        )
    }
}
