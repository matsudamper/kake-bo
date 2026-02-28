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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.StickyHeaderState
import net.matsudamper.money.frontend.common.ui.stickyHeaderScrollable

public data class RootUsageCalendarScreenUiState(
    val event: Event,
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
            val event: DayCellEvent,
        ) : CalendarCell

        public data class DayOfWeek(
            val dayOfWeek: kotlinx.datetime.DayOfWeek,
            val text: String,
        ) : CalendarCell

        public data object Empty : CalendarCell
    }

    public data class CalendarDayItem(
        val title: String,
        val color: Color,
        val event: CalendarDayEvent,
    )

    @Immutable
    public interface DayCellEvent {
        public fun onClick()
    }

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
    stickyHeaderState: StickyHeaderState,
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
                        stickyHeaderState = stickyHeaderState,
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
    stickyHeaderState: StickyHeaderState,
    contentPadding: PaddingValues,
) {
    LazyVerticalGrid(
        modifier = modifier.stickyHeaderScrollable(
            state = stickyHeaderState,
            listState = state,
        ),
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
            .clickable { uiState.event.onClick() }
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
            val textColor = contrastTextColor(item.color)
            Card(
                modifier = Modifier.padding(horizontal = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = item.color,
                ),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { item.event.onClick() }
                        .padding(2.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun contrastTextColor(backgroundColor: Color): Color {
    val luminance = 0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue
    return if (luminance > 0.5) Color.Black else Color.White
}

@Composable
@Preview
private fun Preview() {
    val noOpDayCellEvent = object : RootUsageCalendarScreenUiState.DayCellEvent {
        override fun onClick() {}
    }
    val noOpCalendarDayEvent = object : RootUsageCalendarScreenUiState.CalendarDayEvent {
        override fun onClick() {}
    }
    fun dayItem(title: String, color: Color) = RootUsageCalendarScreenUiState.CalendarDayItem(
        title = title,
        color = color,
        event = noOpCalendarDayEvent,
    )
    val dayOfWeeks = listOf(
        kotlinx.datetime.DayOfWeek.SUNDAY to "日",
        kotlinx.datetime.DayOfWeek.MONDAY to "月",
        kotlinx.datetime.DayOfWeek.TUESDAY to "火",
        kotlinx.datetime.DayOfWeek.WEDNESDAY to "水",
        kotlinx.datetime.DayOfWeek.THURSDAY to "木",
        kotlinx.datetime.DayOfWeek.FRIDAY to "金",
        kotlinx.datetime.DayOfWeek.SATURDAY to "土",
    ).map { (dayOfWeek, text) ->
        RootUsageCalendarScreenUiState.CalendarCell.DayOfWeek(
            dayOfWeek = dayOfWeek,
            text = text,
        )
    }
    val itemsPerDay = mapOf(
        1 to listOf(dayItem("Netflix", Color(0xFFE53935))),
        5 to listOf(
            dayItem("Amazon", Color(0xFF1565C0)),
            dayItem("Spotify", Color(0xFF2E7D32)),
        ),
        10 to listOf(dayItem("電気代", Color(0xFFEF6C00))),
        15 to listOf(
            dayItem("家賃", Color(0xFF6A1B9A)),
            dayItem("水道代", Color(0xFF00695C)),
            dayItem("ガス代", Color(0xFFB71C1C)),
        ),
        20 to listOf(dayItem("スーパー", Color(0xFF1B5E20))),
        25 to listOf(dayItem("コンビニ", Color(0xFF4A148C))),
    )
    val days = (1..31).map { day ->
        RootUsageCalendarScreenUiState.CalendarCell.Day(
            text = day.toString(),
            isToday = day == 15,
            items = itemsPerDay[day].orEmpty().toImmutableList(),
            event = noOpDayCellEvent,
        )
    }
    val emptyCells = List(3) { RootUsageCalendarScreenUiState.CalendarCell.Empty }
    val calendarCells = (dayOfWeeks + emptyCells + days).toImmutableList()
    AppRoot(isDarkTheme = true) {
        RootUsageCalendarScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = RootUsageCalendarScreenUiState(
                event = object : RootUsageCalendarScreenUiState.Event {
                    override suspend fun onViewInitialized() {}
                    override fun refresh() {}
                },
                loadingState = RootUsageCalendarScreenUiState.LoadingState.Loaded(
                    calendarCells = calendarCells,
                    event = object : RootUsageCalendarScreenUiState.LoadedEvent {
                        override fun loadMore() {}
                    },
                ),
            ),
            stickyHeaderState = StickyHeaderState(enterAlways = false),
        )
    }
}
