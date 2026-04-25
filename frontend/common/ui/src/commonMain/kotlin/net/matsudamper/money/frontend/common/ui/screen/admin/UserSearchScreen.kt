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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
    onClickBack: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ユーザー検索",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HtmlTextInput(
                    modifier = Modifier.weight(1f).height(48.dp),
                    placeholder = "ユーザー名を入力",
                    onValueChange = {
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
                                            text = "パスワード上書き",
                                            fontFamily = rememberCustomFontFamily(),
                                        )
                                    },
                                    onClick = { uiState.listener.onClickReplacePassword() },
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    val dialogState = uiState.replacePasswordDialogState
    if (dialogState != null) {
        AlertDialog(
            onDismissRequest = { uiState.listener.onDismissReplacePasswordDialog() },
            title = {
                Text(
                    text = "パスワード上書き: ${dialogState.userName}",
                    fontFamily = rememberCustomFontFamily(),
                )
            },
            text = {
                Column {
                    HtmlTextInput(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        placeholder = "新しいパスワード",
                        onValueChange = {
                            uiState.listener.onPasswordChanged(it.text)
                        },
                        type = KeyboardType.Password,
                    )
                    Text(
                        AdminAddUserUiState.PASSWORD_ALLOW_SYMBOLS_TEXT,
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
                    onClick = { uiState.listener.onClickSubmitReplacePassword() },
                ) {
                    Text(
                        text = "上書き",
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { uiState.listener.onDismissReplacePasswordDialog() },
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
