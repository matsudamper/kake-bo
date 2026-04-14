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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
            when (val state = uiState.loadingState) {
                UploadQueueDebugScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UploadQueueDebugScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        event = uiState.event,
                    )
                }
            }
        },
    )
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    state: UploadQueueDebugScreenUiState.LoadingState.Loaded,
    event: UploadQueueDebugScreenUiState.Event,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@map false
                lastVisibleIndex >= layoutInfo.totalItemsCount - 3
            }
            .distinctUntilChanged()
            .collect { isNearEnd ->
                if (isNearEnd && !state.isLast) {
                    event.onLoadMore()
                }
            }
    }

    if (state.items.isEmpty() && !state.isLoadingMore) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text("キューにアイテムがありません")
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        itemsIndexed(
            items = state.items,
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

        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
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
        is UploadQueueDebugScreenUiState.Status.Failed -> "FAILED" to MaterialTheme.colorScheme.error
    }
    Text(
        modifier = modifier,
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}
