package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffold
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.GridColumn
import net.matsudamper.money.frontend.common.ui.layout.image.ZoomableImageDialog

public data class CalendarDateListScreenUiState(
    val title: String,
    val event: Event,
    val loadingState: LoadingState,
) {
    @Immutable
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val items: ImmutableList<Item>,
            val loadToEnd: Boolean,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class Item(
        val id: String,
        val title: String,
        val date: String,
        val amount: String,
        val category: String?,
        val images: ImmutableList<ImageItem>,
        val event: ItemEvent,
    )

    public data class ImageItem(
        val id: String,
        val url: String,
    )

    @Immutable
    public interface ItemEvent {
        public fun onClick()
    }

    @Immutable
    public interface LoadedEvent {
        public fun loadMore()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
        public fun refresh()
        public fun onClickBack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CalendarDateListScreen(
    modifier: Modifier,
    uiState: CalendarDateListScreenUiState,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(uiState.event) {
        uiState.event.onViewInitialized()
    }
    KakeboScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            kakeboScaffoldListener.onClickTitle()
                        },
                        text = uiState.title,
                    )
                },
                windowInsets = windowInsets,
            )
        },
    ) { paddingValues ->
        val state = rememberPullToRefreshState()
        val coroutineScope = rememberCoroutineScope()
        var isRefreshing by remember { mutableStateOf(false) }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            state = state,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    uiState.event.refresh()
                    delay(1000)
                    isRefreshing = false
                }
            },
        ) {
            when (val loadingState = uiState.loadingState) {
                is CalendarDateListScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = loadingState,
                    )
                }

                is CalendarDateListScreenUiState.LoadingState.Loading -> {
                    LaunchedEffect(Unit) { isRefreshing = false }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier,
    uiState: CalendarDateListScreenUiState.LoadingState.Loaded,
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
    ) {
        items(uiState.items, key = { it.id }) { item ->
            var selectedImageUrl by remember { mutableStateOf<String?>(null) }
            val currentSelectedImageUrl = selectedImageUrl
            if (currentSelectedImageUrl != null) {
                ZoomableImageDialog(
                    imageUrl = currentSelectedImageUrl,
                    onDismissRequest = { selectedImageUrl = null },
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                onClick = item.event::onClick,
            ) {
                GridColumn(
                    modifier = Modifier.fillMaxWidth()
                        .padding(12.dp),
                    horizontalPadding = 8.dp,
                    verticalPadding = 4.dp,
                ) {
                    row {
                        item {
                            Text("タイトル")
                        }
                        item {
                            Text(text = item.title)
                        }
                    }
                    row {
                        item {
                            Text("日付")
                        }
                        item {
                            Text(text = item.date)
                        }
                    }
                    row {
                        item {
                            Text("金額")
                        }
                        item {
                            Text(text = item.amount)
                        }
                    }
                    row {
                        item {
                            Text("カテゴリ")
                        }
                        item {
                            Text(text = item.category.orEmpty())
                        }
                    }
                }
                if (item.images.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(item.images, key = { it.id }) { imageItem ->
                            AsyncImage(
                                model = imageItem.url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable { selectedImageUrl = imageItem.url },
                            )
                        }
                    }
                }
            }
        }
        if (uiState.loadToEnd.not()) {
            item {
                LaunchedEffect(Unit) {
                    uiState.event.loadMore()
                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
        item {
            if (uiState.items.isEmpty() && uiState.loadToEnd) {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("この日の使用用途はありません")
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
