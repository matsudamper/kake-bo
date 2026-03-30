package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil3.compose.SubcomposeAsyncImage
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
        val title: String,
        val date: String,
        val amount: String,
        val category: String?,
        val images: ImmutableList<ImageItem>,
        val event: ItemEvent,
    )

    public data class ImageItem(
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
        public fun onClickAdd()
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
    var fabY by remember { mutableStateOf(0f) }
    val localDensity = LocalDensity.current
    KakeboScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    fabY = coordinates.positionInWindow().y
                },
                onClick = { uiState.event.onClickAdd() },
            ) {
                Icon(Icons.Default.Add, contentDescription = "使用用途を追加")
            }
        },
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
        val localDirection = LocalLayoutDirection.current
        var boxY by remember { mutableStateOf(0f) }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                boxY = coordinates.boundsInWindow().height
            },
            state = state,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    uiState.event.refresh()
                    delay(1000.milliseconds)
                    isRefreshing = false
                }
            },
        ) {
            when (val loadingState = uiState.loadingState) {
                is CalendarDateListScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = loadingState,
                        paddingValues = PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding() + with(localDensity) {
                                (boxY - fabY).coerceAtLeast(0f).toDp()
                            },
                            start = paddingValues.calculateStartPadding(localDirection),
                            end = paddingValues.calculateEndPadding(localDirection),
                        ),
                    )
                }

                is CalendarDateListScreenUiState.LoadingState.Loading -> {
                    LaunchedEffect(Unit) { isRefreshing = false }
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
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
    uiState: CalendarDateListScreenUiState.LoadingState.Loaded,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = paddingValues,
    ) {
        items(uiState.items) { item ->
            ItemCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                item = item,
            )
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

@Composable
private fun ItemCard(
    item: CalendarDateListScreenUiState.Item,
    modifier: Modifier = Modifier,
) {
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    val currentSelectedImageUrl = selectedImageUrl
    if (currentSelectedImageUrl != null) {
        ZoomableImageDialog(
            imageUrl = currentSelectedImageUrl,
            onDismissRequest = { selectedImageUrl = null },
        )
    }
    Card(
        modifier = modifier,
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
                items(item.images) { imageItem ->
                    SubcomposeAsyncImage(
                        model = imageItem.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clickable { selectedImageUrl = imageItem.url },
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        },
                    )
                }
            }
        }
    }
}
