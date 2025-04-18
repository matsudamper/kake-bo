package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.ImmutableList

public data class RootUsageCalendarScreenUiState(
    val event: Event,
    val hostScreenUiState: RootUsageHostScreenUiState,
    val loadingState: LoadingState,
) {
    @Immutable
    public sealed interface LoadingState {
        public data object Loading : LoadingState

        public data class Loaded(
            val calendarCells: ImmutableList<CalendarCell>,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public sealed interface CalendarCell {
        public data class Day(
            val text: String,
            val isToday: Boolean,
            val items: ImmutableList<CalendarDayItem>,
        ) : CalendarCell

        public data class DayOfWeek(
            val dayOfWeek: kotlinx.datetime.DayOfWeek,
            val text: String,
        ) : CalendarCell

        public data object Empty : CalendarCell
    }

    public data class CalendarDayItem(
        val title: String,
        val event: CalendarDayEvent,
    )

    @Immutable
    public interface CalendarDayEvent {
        public fun onClick()
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
        public fun refresh()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RootUsageCalendarScreen(
    modifier: Modifier = Modifier,
    uiState: RootUsageCalendarScreenUiState,
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    val state = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        modifier = modifier,
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
        BoxWithConstraints(modifier) {
            when (uiState.loadingState) {
                is RootUsageCalendarScreenUiState.LoadingState.Loaded -> {
                    var buttonSize: IntSize by remember { mutableStateOf(IntSize.Zero) }
                    val lazyGridState = rememberLazyGridState()
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState.loadingState,
                        state = lazyGridState,
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = with(density) { buttonSize.height.toDp() },
                        ),
                    )
                }

                is RootUsageCalendarScreenUiState.LoadingState.Loading -> {
                    LaunchedEffect(Unit) { isRefreshing = false }
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: RootUsageCalendarScreenUiState.LoadingState.Loaded,
    state: LazyGridState,
    contentPadding: PaddingValues,
) {
    LazyVerticalGrid(
        modifier = modifier,
        state = state,
        columns = GridCells.Fixed(7),
        contentPadding = contentPadding,
    ) {
        items(uiState.calendarCells) { cell ->
            when (cell) {
                is RootUsageCalendarScreenUiState.CalendarCell.Day -> {
                    CalendarCell(
                        modifier = Modifier.heightIn(min = 100.dp),
                        uiState = cell,
                    )
                }

                is RootUsageCalendarScreenUiState.CalendarCell.Empty -> Unit
                is RootUsageCalendarScreenUiState.CalendarCell.DayOfWeek -> {
                    Text(
                        modifier = Modifier.fillMaxSize()
                            .padding(2.dp),
                        text = cell.text,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    modifier: Modifier = Modifier,
    uiState: RootUsageCalendarScreenUiState.CalendarCell.Day,
) {
    Column(
        modifier = modifier
            .padding(vertical = 2.dp),
    ) {
        Text(
            modifier = Modifier
                .padding(2.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (uiState.isToday) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.Transparent
                    },
                )
                .padding(2.dp),
            color = if (uiState.isToday) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                Color.Unspecified
            },
            text = uiState.text,
            style = MaterialTheme.typography.titleSmall,
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .height(1.dp),
        )
        uiState.items.forEach { item ->
            Spacer(Modifier.height(2.dp))
            Card(
                modifier = Modifier.padding(horizontal = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { item.event.onClick() }
                        .padding(2.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
