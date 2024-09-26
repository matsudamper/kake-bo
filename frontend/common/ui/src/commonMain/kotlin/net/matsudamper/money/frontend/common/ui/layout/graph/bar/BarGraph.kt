package net.matsudamper.money.frontend.common.ui.layout.graph.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import net.matsudamper.money.frontend.common.base.ImmutableList

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
    uiState: BarGraphUiState,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.secondary,
) {
    var maxValue by remember { mutableLongStateOf(0) }
    LaunchedEffect(uiState) {
        uiState.items.map { item ->
            val value = item.items.sumOf { it.value }
            maxValue = maxValue.coerceAtLeast(value)
        }
    }
    if (maxValue != 0L) {
        Column(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Bottom,
            ) {
                for ((index, item) in uiState.items.withIndex()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                    ) {
                        if (index == 0) {
                            Box(
                                modifier = Modifier.fillMaxHeight()
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(
                                            constraints.copy(
                                                maxWidth = Constraints.Infinity,
                                            ),
                                        )
                                        layout(constraints.maxWidth, constraints.maxHeight) {
                                            placeable.place(0, 0)
                                        }
                                    },
                            ) {
                                val maxValueString = maxValue.toString()
                                    .reversed()
                                    .chunked(3)
                                    .joinToString(",")
                                    .reversed()
                                Text(
                                    text = maxValueString,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        SingleBarGraph(
                            modifier = Modifier
                                .fillMaxHeight(),
                            maxValue = maxValue,
                            items = item.items,
                            onClick = { item.event.onClick() },
                        )
                    }
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
}

@Composable
private fun SingleBarGraph(
    maxValue: Long,
    items: ImmutableList<BarGraphUiState.Item>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val containerHeight = this.maxHeight
        val containerWidth = this.maxWidth
        Column(
            modifier = Modifier.fillMaxSize()
                .clickable { onClick() },
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
