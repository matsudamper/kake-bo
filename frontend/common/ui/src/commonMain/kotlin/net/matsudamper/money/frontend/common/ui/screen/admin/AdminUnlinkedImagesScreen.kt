package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import net.matsudamper.money.frontend.common.ui.layout.image.ImageLoadingPlaceholder
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminUnlinkedImagesScreen(
    modifier: Modifier = Modifier,
    uiState: AdminUnlinkedImagesScreenUiState,
    onClickBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    when (val state = uiState.screenState) {
        AdminUnlinkedImagesScreenUiState.ScreenState.Loading -> {
            LoadingScreen(modifier = modifier, onClickBack = onClickBack)
        }

        AdminUnlinkedImagesScreenUiState.ScreenState.Error -> {
            ErrorScreen(modifier = modifier, onClickBack = onClickBack, onClickRetry = { uiState.event.onClickRetry() })
        }

        is AdminUnlinkedImagesScreenUiState.ScreenState.MonthList -> {
            MonthListScreen(
                modifier = modifier,
                state = state,
                onClickBack = onClickBack,
                onClickMonth = { uiState.event.onClickMonth(it) },
            )
        }

        is AdminUnlinkedImagesScreenUiState.ScreenState.MonthDetail -> {
            MonthDetailScreen(
                modifier = modifier,
                state = state,
                onClickBack = { uiState.event.onClickBack() },
                event = uiState.event,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier,
    onClickBack: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                title = {
                    Text(text = "未紐づき画像", fontFamily = rememberCustomFontFamily())
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorScreen(
    modifier: Modifier = Modifier,
    onClickBack: () -> Unit,
    onClickRetry: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                title = {
                    Text(text = "未紐づき画像", fontFamily = rememberCustomFontFamily())
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("データの取得に失敗しました")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onClickRetry) {
                    Text("再試行")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthListScreen(
    modifier: Modifier = Modifier,
    state: AdminUnlinkedImagesScreenUiState.ScreenState.MonthList,
    onClickBack: () -> Unit,
    onClickMonth: (String) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                title = {
                    Text(text = "未紐づき画像 - 月別", fontFamily = rememberCustomFontFamily())
                },
            )
        },
    ) { innerPadding ->
        if (state.months.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("未紐づき画像はありません")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = state.months,
                    key = { it.yearMonth },
                ) { month ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onClickMonth(month.yearMonth) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = month.yearMonth,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = rememberCustomFontFamily(),
                            )
                            Text(
                                text = "${month.count}件",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = rememberCustomFontFamily(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthDetailScreen(
    modifier: Modifier = Modifier,
    state: AdminUnlinkedImagesScreenUiState.ScreenState.MonthDetail,
    onClickBack: () -> Unit,
    event: AdminUnlinkedImagesScreenUiState.Event,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                title = {
                    Text(
                        text = "${state.yearMonth} (${state.items.size}件)",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        TextButton(onClick = { event.onClickSelectAll() }) {
                            Text("全選択", fontFamily = rememberCustomFontFamily())
                        }
                        TextButton(onClick = { event.onClickDeselectAll() }) {
                            Text("全解除", fontFamily = rememberCustomFontFamily())
                        }
                        IconButton(
                            onClick = { event.onClickDeleteSelected() },
                            enabled = state.selectedIds.isNotEmpty() && !state.isDeleting,
                        ) {
                            if (state.isDeleting) {
                                CircularProgressIndicator()
                            } else {
                                Icon(Icons.Default.Delete, contentDescription = "削除")
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("未紐づき画像はありません")
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (state.selectedIds.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        text = "${state.selectedIds.size}件選択中",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = rememberCustomFontFamily(),
                    )
                }
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(
                        items = state.items,
                        key = { it.id },
                    ) { item ->
                        val isSelected = state.selectedIds.contains(item.id)
                        UnlinkedImageItem(
                            modifier = Modifier.fillMaxWidth(),
                            item = item,
                            isSelected = isSelected,
                            onClick = { event.onToggleImageSelection(item.id) },
                        )
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
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small),
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { ImageLoadingPlaceholder() },
            )
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
}
