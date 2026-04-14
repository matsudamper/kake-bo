package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.DropDownMenuButton
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold

@Composable
public fun UploadQueueDebugScreen(
    modifier: Modifier = Modifier,
    uiState: UploadQueueDebugScreenUiState,
    windowInsets: PaddingValues,
) {
    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = { Text("画像アップロードキュー") },
                windowInsets = windowInsets,
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                StatusFilterRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    uiState = uiState,
                )
                if (uiState.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("一致はありません")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = uiState.items,
                            key = { _, item -> item.id },
                        ) { index, item ->
                            if (index > 0) {
                                HorizontalDivider()
                            }
                            QueueItemRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { uiState.event.onClickItem(item) },
                                item = item,
                            )
                        }
                    }
                }
            }
        },
    )

    val dialogItem = uiState.errorDialogItem
    if (dialogItem != null) {
        ErrorDetailDialog(
            item = dialogItem,
            onDismiss = { uiState.event.onDismissErrorDialog() },
        )
    }
}

@Composable
private fun ErrorDetailDialog(
    item: UploadQueueDebugScreenUiState.Item,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val errorText = buildString {
        if (item.errorMessage != null) {
            appendLine(item.errorMessage)
        }
        if (item.stackTrace != null) {
            appendLine()
            append(item.stackTrace)
        }
    }.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("エラー詳細") },
        text = {
            Text(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                text = errorText.ifEmpty { "エラー情報はありません" },
                style = MaterialTheme.typography.bodySmall,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(errorText))
                },
            ) {
                Text("コピー")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
    )
}

@Composable
private fun StatusFilterRow(
    modifier: Modifier = Modifier,
    uiState: UploadQueueDebugScreenUiState,
) {
    Box(modifier = modifier) {
        DropDownMenuButton(
            onClick = { uiState.event.onClickStatusFilter() },
        ) {
            Text(
                text = when (uiState.selectedStatusFilter) {
                    UploadQueueDebugScreenUiState.StatusFilter.All -> "すべて"
                    UploadQueueDebugScreenUiState.StatusFilter.Pending -> "PENDING"
                    UploadQueueDebugScreenUiState.StatusFilter.Uploading -> "UPLOADING"
                    UploadQueueDebugScreenUiState.StatusFilter.Completed -> "COMPLETED"
                    UploadQueueDebugScreenUiState.StatusFilter.Failed -> "FAILED"
                },
            )
        }
        DropdownMenu(
            expanded = uiState.statusFilterExpanded,
            onDismissRequest = { uiState.event.onDismissStatusFilter() },
        ) {
            UploadQueueDebugScreenUiState.StatusFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (filter) {
                                UploadQueueDebugScreenUiState.StatusFilter.All -> "すべて"
                                UploadQueueDebugScreenUiState.StatusFilter.Pending -> "PENDING"
                                UploadQueueDebugScreenUiState.StatusFilter.Uploading -> "UPLOADING"
                                UploadQueueDebugScreenUiState.StatusFilter.Completed -> "COMPLETED"
                                UploadQueueDebugScreenUiState.StatusFilter.Failed -> "FAILED"
                            },
                        )
                    },
                    onClick = { uiState.event.onSelectStatusFilter(filter) },
                )
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    modifier: Modifier = Modifier,
    item: UploadQueueDebugScreenUiState.Item,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(status = item.status)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "moneyUsageId: ${item.moneyUsageId}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "id: ${item.id.take(16)}…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "createdAt: ${item.createdAt}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.errorMessage != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "error: ${item.errorMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.workManagerId != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "workManagerId: ${item.workManagerId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusBadge(
    modifier: Modifier = Modifier,
    status: UploadQueueDebugScreenUiState.Status,
) {
    val (label, color) = when (status) {
        UploadQueueDebugScreenUiState.Status.Pending -> "PENDING" to MaterialTheme.colorScheme.primary
        UploadQueueDebugScreenUiState.Status.Uploading -> "UPLOADING" to MaterialTheme.colorScheme.tertiary
        UploadQueueDebugScreenUiState.Status.Completed -> "COMPLETED" to MaterialTheme.colorScheme.secondary
        is UploadQueueDebugScreenUiState.Status.Failed -> "FAILED" to MaterialTheme.colorScheme.error
    }
    Text(
        modifier = modifier,
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}
