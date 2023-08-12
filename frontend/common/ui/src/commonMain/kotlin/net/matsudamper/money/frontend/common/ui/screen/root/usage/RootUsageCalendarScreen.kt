package net.matsudamper.money.frontend.common.ui.screen.root.usage

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.layout.ScrollButton
import net.matsudamper.money.frontend.common.ui.layout.ScrollButtonDefaults

public data class RootUsageCalendarScreenUiState(
    val event: Event,
    val loadingState: LoadingState,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public data class Loaded(
            val calendarCells: ImmutableList<CalendarCell>,
            val event: LoadedEvent,
        ) : LoadingState
    }

    public sealed interface CalendarCell {
        public data class Day(
            val text: String,
            val items: ImmutableList<CalendarDayItem>,
        ) : CalendarCell

        public object Empty : CalendarCell
    }

    public data class CalendarDayItem(
        val title: String,
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
        public fun onClickAdd()
    }
}

@Composable
public fun RootUsageCalendarScreen(
    modifier: Modifier = Modifier,
    uiState: RootUsageCalendarScreenUiState,
) {
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    BoxWithConstraints(modifier) {
        val height = maxHeight
        when (uiState.loadingState) {
            is RootUsageCalendarScreenUiState.LoadingState.Loaded -> {
                val lazyListState = rememberLazyListState()
                LoadedContent(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState.loadingState,
                    paddingValues = PaddingValues(
                        end = ScrollButtonDefaults.scrollButtonHorizontalPadding * 2 + ScrollButtonDefaults.scrollButtonSize,
                    ),
                    lazyListState = lazyListState,
                )

                ScrollButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(ScrollButtonDefaults.scrollButtonHorizontalPadding)
                        .width(ScrollButtonDefaults.scrollButtonSize),
                    scrollState = lazyListState,
                    scrollSize = with(density) {
                        height.toPx() * 0.7f
                    },
                    animationSpec = spring(
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }

            is RootUsageCalendarScreenUiState.LoadingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = ScrollButtonDefaults.scrollButtonHorizontalPadding * 2 + ScrollButtonDefaults.scrollButtonSize)
                .padding(bottom = 24.dp, end = 12.dp),
            onClick = { uiState.event.onClickAdd() },
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "add money usage",
            )
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: RootUsageCalendarScreenUiState.LoadingState.Loaded,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
    ) {
        items(uiState.calendarCells) { cell ->
            Box(modifier = Modifier.heightIn(min = 100.dp)) {
                when (cell) {
                    is RootUsageCalendarScreenUiState.CalendarCell.Day -> {
                        CalendarCell(
                            uiState = cell,
                        )
                    }

                    RootUsageCalendarScreenUiState.CalendarCell.Empty -> Unit
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
    Column(modifier = modifier) {
        Text(uiState.text)
        Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
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
