package net.matsudamper.money.frontend.common.ui.screen.root.settings

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                title = { Text("アップロードキュー") },
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
                        Text("キューにアイテムがありません")
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
                                modifier = Modifier.fillMaxWidth(),
                                item = item,
                            )
                        }
                    }
                }
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
        OutlinedButton(
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
