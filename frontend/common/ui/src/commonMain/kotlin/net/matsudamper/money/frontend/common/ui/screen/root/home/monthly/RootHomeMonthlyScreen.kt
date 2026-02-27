package net.matsudamper.money.frontend.common.ui.screen.root.home.monthly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChart
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChartItem
import net.matsudamper.money.frontend.common.ui.screen.root.home.SortSection
import net.matsudamper.money.frontend.common.ui.screen.root.home.SortSectionOrder
import net.matsudamper.money.frontend.common.ui.screen.root.home.SortSectionType
import org.jetbrains.compose.ui.tooling.preview.Preview

public data class RootHomeMonthlyScreenUiState(
    val loadingState: LoadingState,
    val event: Event,
    val currentSortType: SortSectionType,
    val sortOrder: SortSectionOrder,
) {
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data object Error : LoadingState

        public data class Loaded(
            val yearMonth: String,
            val totalAmount: String,
            val items: ImmutableList<Item>,
            val pieChartItems: ImmutableList<PieChartItem>,
            val hasMoreItem: Boolean,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public data class Item(
        val title: String,
        val amount: String,
        val date: String,
        val category: String,
        val event: ItemEvent,
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
        public suspend fun onViewInitialized()
        public fun onSortTypeChanged(sortType: SortSectionType)
        public fun onSortOrderChanged(order: SortSectionOrder)
    }
}

@Composable
public fun RootHomeMonthlyScreen(
    uiState: RootHomeMonthlyScreenUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    when (val loadingState = uiState.loadingState) {
        is RootHomeMonthlyScreenUiState.LoadingState.Loaded -> {
            LoadedContent(
                modifier = modifier.fillMaxSize(),
                loadingState = loadingState,
                uiState = uiState,
            )
        }

        RootHomeMonthlyScreenUiState.LoadingState.Loading -> {
            Box(modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        RootHomeMonthlyScreenUiState.LoadingState.Error -> {
            LoadingErrorContent(
                modifier = modifier.fillMaxWidth(),
                onClickRetry = {
                    // TODO
                },
            )
        }
    }
}

@Composable
private fun LoadedContent(
    loadingState: RootHomeMonthlyScreenUiState.LoadingState.Loaded,
    uiState: RootHomeMonthlyScreenUiState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val lazyListState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(
                bottom = 8.dp,
                top = 8.dp,
                start = 8.dp,
                end = 8.dp,
            ),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = loadingState.yearMonth,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = loadingState.totalAmount,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            item {
                if (loadingState.pieChartItems.isNotEmpty()) {
                    PieChart(
                        items = loadingState.pieChartItems,
                        title = "カテゴリ別支出",
                        modifier = Modifier.fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }
            item {
                SortSection(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    onSortTypeChanged = { type ->
                        uiState.event.onSortTypeChanged(type)
                    },
                    currentSortType = uiState.currentSortType,
                    onSortOrderChanged = { order ->
                        uiState.event.onSortOrderChanged(order)
                    },
                    sortOrderType = uiState.sortOrder,
                )
            }
            items(loadingState.items) { item ->
                Card(
                    modifier = Modifier.padding(vertical = 2.dp),
                    onClick = {
                        item.event.onClick()
                    },
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.bodyMedium,
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                text = item.date,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = item.title,
                                    maxLines = 3,
                                )
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.Bottom)
                                        .height(IntrinsicSize.Max)
                                        .requiredWidthIn(min = 80.dp),
                                    text = item.category,
                                    maxLines = 1,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    modifier = Modifier
                                        .align(Alignment.Bottom)
                                        .height(IntrinsicSize.Max)
                                        .requiredWidthIn(min = 60.dp),
                                    maxLines = 1,
                                    text = item.amount,
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                    }
                }
            }
            if (loadingState.hasMoreItem) {
                item {
                    LaunchedEffect(Unit) {
                        loadingState.event.loadMore()
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 16.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun RootHomeMonthlyScreenPreview() {
    val noOpItemEvent = object : RootHomeMonthlyScreenUiState.ItemEvent {
        override fun onClick() {}
    }
    AppRoot(isDarkTheme = false) {
        RootHomeMonthlyScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = RootHomeMonthlyScreenUiState(
                loadingState = RootHomeMonthlyScreenUiState.LoadingState.Loaded(
                    yearMonth = "2026年2月",
                    totalAmount = "¥128,450",
                    items = listOf(
                        RootHomeMonthlyScreenUiState.Item(
                            title = "Amazon.co.jp",
                            amount = "¥3,280",
                            date = "2026/02/25",
                            category = "ショッピング",
                            event = noOpItemEvent,
                        ),
                        RootHomeMonthlyScreenUiState.Item(
                            title = "スーパーマーケット",
                            amount = "¥5,430",
                            date = "2026/02/24",
                            category = "食費",
                            event = noOpItemEvent,
                        ),
                        RootHomeMonthlyScreenUiState.Item(
                            title = "電気料金",
                            amount = "¥8,200",
                            date = "2026/02/20",
                            category = "光熱費",
                            event = noOpItemEvent,
                        ),
                    ).toImmutableList(),
                    pieChartItems = listOf(
                        PieChartItem(color = Color(0xFF558B2F), title = "食費", value = 45000),
                        PieChartItem(color = Color(0xFF1565C0), title = "光熱費", value = 25000),
                        PieChartItem(color = Color(0xFFE65100), title = "ショッピング", value = 32000),
                    ).toImmutableList(),
                    hasMoreItem = false,
                    event = object : RootHomeMonthlyScreenUiState.LoadedEvent {
                        override fun loadMore() {}
                    },
                ),
                event = object : RootHomeMonthlyScreenUiState.Event {
                    override suspend fun onViewInitialized() {}
                    override fun onSortTypeChanged(sortType: SortSectionType) {}
                    override fun onSortOrderChanged(order: SortSectionOrder) {}
                },
                currentSortType = SortSectionType.Date,
                sortOrder = SortSectionOrder.Descending,
            ),
            windowInsets = PaddingValues(),
        )
    }
}
