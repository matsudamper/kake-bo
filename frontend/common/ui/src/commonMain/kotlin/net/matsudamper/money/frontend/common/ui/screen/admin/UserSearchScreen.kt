package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.matsudamper.money.frontend.common.ui.layout.TextField
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
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
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        text = uiState.searchQuery,
                        placeholder = "ユーザー名を入力",
                        onValueChange = {
                            uiState.listener.onSearchQueryChanged(it)
                        },
                        type = TextFieldType.Text,
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
                    items(uiState.searchResults) { result ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { uiState.listener.onClickUser(result.name) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "ID: ${result.userId.value} / Name: ${result.name}",
                                    fontFamily = rememberCustomFontFamily(),
                                )
                            }
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (uiState.hasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                uiState.listener.onClickLoadMore()
                                while (isActive) {
                                    delay(500)
                                    uiState.listener.onClickLoadMore()
                                }
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.selectedUserName != null) {
        Dialog(
            onDismissRequest = { uiState.listener.onDismissUserMenu() },
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.selectedUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = rememberCustomFontFamily(),
                        )
                        IconButton(onClick = { uiState.listener.onDismissUserMenu() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { uiState.listener.onClickReplacePassword() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "パスワード上書き",
                                fontFamily = rememberCustomFontFamily(),
                            )
                        }
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
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        text = dialogState.password,
                        placeholder = "新しいパスワード",
                        onValueChange = {
                            uiState.listener.onPasswordChanged(it)
                        },
                        type = TextFieldType.Password,
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
