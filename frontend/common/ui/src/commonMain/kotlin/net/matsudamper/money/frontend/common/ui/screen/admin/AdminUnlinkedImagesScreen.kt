package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog
import net.matsudamper.money.frontend.common.ui.layout.image.ImageLoadingPlaceholder
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AdminUnlinkedImagesScreen(
    modifier: Modifier = Modifier,
    uiState: AdminUnlinkedImagesScreenUiState,
    onClickBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    uiState.deleteDialog?.let { deleteDialog ->
        AlertDialog(
            title = { Text("画像を削除しますか？") },
            description = {
                Column {
                    Text("選択中の ${deleteDialog.selectedCount} 件の画像を削除します。")
                    deleteDialog.errorMessage?.let { errorMessage ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            positiveButton = {
                if (deleteDialog.isLoading) {
                    Text("削除中...")
                } else {
                    Text("削除")
                }
            },
            negativeButton = { Text("キャンセル") },
            onClickPositive = { deleteDialog.event.onConfirm() },
            onClickNegative = { deleteDialog.event.onCancel() },
            onDismissRequest = { deleteDialog.event.onDismiss() },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            val loadedState = uiState.loadingState as? AdminUnlinkedImagesScreenUiState.LoadingState.Loaded
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(painter = painterResource(Res.drawable.ic_arrow_back), contentDescription = "戻る")
                    }
                },
                title = {
                    val totalCount = loadedState?.totalCount
                    val titleText = if (totalCount != null) {
                        "未紐づき画像 ($totalCount)"
                    } else {
                        "未紐づき画像"
                    }
                    Text(
                        text = titleText,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                actions = {
                    if (loadedState != null) {
                        TextButton(
                            onClick = { uiState.event.onClickSelectAll() },
                            enabled = loadedState.items.isNotEmpty() &&
                                loadedState.isLoadingMore.not() &&
                                loadedState.isDeleting.not() &&
                                loadedState.isSelectingAll.not(),
                        ) {
                            Text(
                                if (loadedState.isAllSelected) {
                                    "全解除"
                                } else {
                                    "全選択"
                                },
                            )
                        }
                        TextButton(
                            onClick = { uiState.event.onClickDelete() },
                            enabled = loadedState.selectedCount > 0 &&
                                loadedState.isDeleting.not() &&
                                loadedState.isSelectingAll.not(),
                        ) {
                            Text("削除")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState.loadingState) {
            AdminUnlinkedImagesScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            AdminUnlinkedImagesScreenUiState.LoadingState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("データの取得に失敗しました")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { uiState.event.onClickRetry() }) {
                            Text("再試行")
                        }
                    }
                }
            }

            is AdminUnlinkedImagesScreenUiState.LoadingState.Loaded -> {
                if (state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("未紐づき画像はありません")
                    }
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        columns = GridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(
                            items = state.items,
                            key = { it.id },
                        ) { item ->
                            UnlinkedImageItem(
                                modifier = Modifier.fillMaxWidth(),
                                item = item,
                            )
                        }
                        if (state.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator()
                                    } else {
                                        OutlinedButton(onClick = { uiState.event.onClickLoadMore() }) {
                                            Text("さらに読み込む")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlinkedImageItem(
    modifier: Modifier = Modifier,
    item: AdminUnlinkedImagesScreenUiState.Item,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable { item.event.onClickSelect() },
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { ImageLoadingPlaceholder() },
            )
            Checkbox(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                checked = item.isSelected,
                onCheckedChange = { item.event.onClickSelect() },
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "UserID: ${item.userId}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
        Text(
            text = "UserName: ${item.userName}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
    }
}
