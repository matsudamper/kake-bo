package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.layout.GridColumn

public data class RootUsageListScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
    val hostScreenUiState: RootUsageHostScreenUiState,
) {
    @Immutable
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val loadToEnd: Boolean,
            val items: ImmutableList<Item>,
            val event: LoadedEvent,
        ) : LoadingState
    }

    @Immutable
    public sealed interface Item {
        public data class Usage(
            val title: String,
            val date: String,
            val amount: String,
            val category: String?,
            val event: ItemEvent,
        ) : Item

        public data class Title(
            val title: String,
        ) : Item
    }

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
        public suspend fun onViewInitialized()
    }
}

@Composable
public fun RootUsageListScreen(
    modifier: Modifier = Modifier,
    uiState: RootUsageListScreenUiState,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    BoxWithConstraints(modifier) {
        when (uiState.loadingState) {
            is RootUsageListScreenUiState.LoadingState.Loaded -> {
                val lazyListState = rememberLazyListState()
                LoadedContent(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState.loadingState,
                    paddingValues = PaddingValues(),
                    lazyListState = lazyListState,
                )
            }

            is RootUsageListScreenUiState.LoadingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: RootUsageListScreenUiState.LoadingState.Loaded,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
        ),
    ) {
        item {
            Spacer(modifier = Modifier.heightIn(paddingValues.calculateTopPadding()))
        }
        items(
            items = uiState.items,
            contentType = { it::class },
        ) { item ->
            when (item) {
                is RootUsageListScreenUiState.Item.Title -> {
                    ListItemTitle(
                        modifier = Modifier.fillMaxWidth(),
                        item = item,
                    )
                }

                is RootUsageListScreenUiState.Item.Usage -> {
                    ListItemUsage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .padding(start = 12.dp),
                        item = item,
                    )
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
            Spacer(modifier = Modifier.heightIn(paddingValues.calculateBottomPadding()))
        }
    }
}

@Composable
private fun ListItemTitle(
    modifier: Modifier = Modifier,
    item: RootUsageListScreenUiState.Item.Title,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = item.title,
            style = MaterialTheme.typography.titleLarge,
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth().height(1.dp))
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ListItemUsage(
    modifier: Modifier = Modifier,
    item: RootUsageListScreenUiState.Item.Usage,
) {
    Card(
        modifier = modifier,
        onClick = item.event::onClick,
    ) {
        GridColumn(
            modifier =
                Modifier.fillMaxWidth()
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
    }
}
