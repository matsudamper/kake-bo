package net.matsudamper.money.frontend.common.ui.layout.graph.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList

internal class BarGraphConfig(
    density: Density,
) {
    val maxBarWidth = with(density) { 42.dp.toPx() }
    val minSpaceWidth = with(density) { 2.dp.toPx() }
    val defaultSpaceWidth = with(density) { 16.dp.toPx() }
    val multilineLabelHeightPadding = with(density) { 4.dp.toPx() }
    val graphAndLabelPadding = with(density) { 8.dp.toPx() }
}

public data class BarGraphUiState(
    val items: ImmutableList<PeriodData>,
) {
    public data class PeriodData(
        val year: Int,
        val month: Int,
        val items: ImmutableList<Item>,
        val total: Long,
        val event: PeriodDataEvent,
    )

    @Immutable
    public interface PeriodDataEvent {
        public fun onClick()
    }

    public data class Item(
        val color: Color,
        val title: String,
        val value: Long,
    )
}

@Composable
internal fun BarGraph(
    modifier: Modifier = Modifier,
    uiState: BarGraphUiState,
    contentColor: Color = MaterialTheme.colorScheme.secondary,
) {
    var maxValue by remember { mutableLongStateOf(0) }
    LaunchedEffect(uiState) {
        uiState.items.map { item ->
            val value = item.items.sumOf { it.value }
            maxValue = maxValue.coerceAtLeast(value)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (item in uiState.items) {
                SingleBarGraph(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    maxValue = maxValue,
                    items = item.items,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            for ((index, item) in uiState.items.withIndex()) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    maxWidth = Constraints.Infinity,
                                ),
                            )
                            val y = if (index % 2 == 0) {
                                0
                            } else {
                                placeable.height
                            }
                            layout(placeable.width, placeable.height * 2) {
                                placeable.place(
                                    x = (constraints.maxWidth - placeable.width) / 2,
                                    y = y,
                                )
                            }
                            layout(placeable.width, placeable.height * 2) {
                                placeable.place(0, y)
                            }
                        },
                    text = if (item.month == 0 || index == 0) {
                        "${item.year}/${item.month}"
                    } else {
                        item.month.toString()
                    },
                    color = contentColor,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SingleBarGraph(
    maxValue: Long,
    items: ImmutableList<BarGraphUiState.Item>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    if (maxValue != 0L) {
        BoxWithConstraints(modifier = modifier) {
            val containerHeight = this.maxHeight
            val containerWidth = this.maxWidth
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                for (item in items.reversed()) {
                    Box(
                        modifier = Modifier
                            .background(item.color)
                            .requiredWidth(containerWidth)
                            .requiredHeight(
                                with(density) {
                                    val heightPx = containerHeight.toPx() * item.value / maxValue
                                    heightPx.toDp()
                                },
                            ),
                    )
                }
            }
        }
    }
}
