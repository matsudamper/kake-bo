package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChart
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChartItem

public data class RootHomeMonthlyCategoryScreenUiState(
    val loadingState: LoadingState,
    val headerTitle: String,
    val event: Event,
    val scaffoldListener: RootScreenScaffoldListener,
    val currentSortType: SortType,
    val sortOrder: SortOrder,
) {
    public enum class SortType {
        Date,
        Amount,
    }

    public enum class SortOrder {
        Ascending,
        Descending,
    }

    public data class Item(
        val title: String,
        val amount: String,
        val subCategory: String,
        val date: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClick()
        }
    }

    public sealed interface LoadingState {
        public data class Loaded(
            val items: List<Item>,
            val hasMoreItem: Boolean,
            val event: LoadedEvent,
            val pieChartItems: ImmutableList<PieChartItem>,
            val pieChartTitle: String,
        ) : LoadingState

        public data object Loading : LoadingState

        public data object Error : LoadingState
    }

    @Immutable
    public interface LoadedEvent {
        public fun loadMore()
    }

    @Immutable
    public interface Event {
        public fun onViewInitialized()
        public fun onSortTypeChanged(sortType: SortType)
        public fun onSortOrderChanged(order: SortOrder)
    }
}

@Composable
public fun RootHomeMonthlyCategoryScreen(
    uiState: RootHomeMonthlyCategoryScreenUiState,
    modifier: Modifier = Modifier,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Home,
        listener = uiState.scaffoldListener,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(uiState.headerTitle)
                },
                windowInsets = windowInsets,
            )
        },
    ) {
        when (val loadingState = uiState.loadingState) {
            is RootHomeMonthlyCategoryScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.fillMaxWidth(),
                    loadingState = loadingState,
                    uiState = uiState,
                )
            }

            RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            RootHomeMonthlyCategoryScreenUiState.LoadingState.Error -> {
                LoadingErrorContent(
                    modifier = Modifier,
                    onClickRetry = { /* TODO */ },
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    loadingState: RootHomeMonthlyCategoryScreenUiState.LoadingState.Loaded,
    uiState: RootHomeMonthlyCategoryScreenUiState,
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
                if (loadingState.pieChartItems.isNotEmpty()) {
                    PieChart(
                        items = loadingState.pieChartItems,
                        title = loadingState.pieChartTitle,
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
                ListItem(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                )
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
private fun ListItem(
    item: RootHomeMonthlyCategoryScreenUiState.Item,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
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
                        text = item.subCategory,
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

@Composable
private fun SortSection(
    currentSortType: RootHomeMonthlyCategoryScreenUiState.SortType,
    sortOrderType: RootHomeMonthlyCategoryScreenUiState.SortOrder,
    onSortTypeChanged: (RootHomeMonthlyCategoryScreenUiState.SortType) -> Unit,
    onSortOrderChanged: (RootHomeMonthlyCategoryScreenUiState.SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val maxWidth = this.maxWidth
        Layout(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "並び替え:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Button(
                        onClick = {
                            onSortTypeChanged(RootHomeMonthlyCategoryScreenUiState.SortType.Date)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentSortType == RootHomeMonthlyCategoryScreenUiState.SortType.Date) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("日付順")
                    }
                    Button(
                        onClick = {
                            onSortTypeChanged(RootHomeMonthlyCategoryScreenUiState.SortType.Amount)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentSortType == RootHomeMonthlyCategoryScreenUiState.SortType.Amount) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Text("金額順")
                    }
                }
                var isExpanded by remember { mutableStateOf(false) }
                TextButton(
                    onClick = {
                        isExpanded = !isExpanded
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = null,
                    )
                    DropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = {
                            isExpanded = false
                        },
                        modifier = Modifier,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("昇順")
                            },
                            onClick = {
                                onSortOrderChanged(RootHomeMonthlyCategoryScreenUiState.SortOrder.Ascending)
                                isExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text("降順")
                            },
                            onClick = {
                                onSortOrderChanged(RootHomeMonthlyCategoryScreenUiState.SortOrder.Descending)
                                isExpanded = false
                            },
                        )
                    }
                    Text(
                        when (sortOrderType) {
                            RootHomeMonthlyCategoryScreenUiState.SortOrder.Ascending -> "昇順"
                            RootHomeMonthlyCategoryScreenUiState.SortOrder.Descending -> "降順"
                        },
                    )
                }
            },
        ) { measurables, constraints ->
            val typePlaceable = measurables[0].measure(constraints)
            val orderPlaceable = measurables[1].measure(constraints)

            val height = max(typePlaceable.height, orderPlaceable.height)
            layout(
                width = maxWidth.roundToPx(),
                height = height,
            ) {
                typePlaceable.place(
                    x = 0,
                    y = Alignment.CenterVertically.align(typePlaceable.height, height),
                )
                if (typePlaceable.width + orderPlaceable.width > maxWidth.toPx()) {
                    orderPlaceable.place(
                        x = typePlaceable.width,
                        y = Alignment.CenterVertically.align(orderPlaceable.height, height),
                    )
                } else {
                    orderPlaceable.place(
                        x = maxWidth.roundToPx() - orderPlaceable.width,
                        y = Alignment.CenterVertically.align(orderPlaceable.height, height),
                    )
                }
            }
        }
    }
}
