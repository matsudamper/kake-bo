package net.matsudamper.money.frontend.common.ui.layout.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf

public data class BarGraphUiState(
    val items: ImmutableList<PeriodData>,
) {
    public data class PeriodData(
        val year: Int,
        val month: Int,
        val items: ImmutableList<Item>,
        val total: Long,
    )

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
    val latestUiState by rememberUpdatedState(uiState)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 2 + 12)
    val config = remember { Config(density) }
    val maxTotalValue by remember { derivedStateOf { latestUiState.items.maxOfOrNull { it.total } ?: 0 } }
    val textMeasureCache = remember(textMeasurer) {
        TextMeasureCache(
            textMeasurer = textMeasurer,
        )
    }
    LaunchedEffect(latestUiState.items) {
        textMeasureCache.updateItems(latestUiState.items)
    }

    val yLabelMaxWidth by derivedStateOf {
        with(density) {
            textMeasureCache.minTextMeasureResult.size.width
                .coerceAtLeast(textMeasureCache.maxTextMeasureResult.size.width)
                .plus(8.dp.toPx())
        }
    }

    BoxWithConstraints(modifier = modifier) {
        var cursorPosition by remember { mutableStateOf(Offset.Zero) }
        val graphWidth by remember(density, maxWidth) {
            derivedStateOf {
                with(density) { maxWidth.toPx() } - yLabelMaxWidth
            }
        }
        val graphBaseX = yLabelMaxWidth

        val spaceWidth by remember(config) {
            derivedStateOf {
                if ((config.maxBarWidth * latestUiState.items.size) + (config.minSpaceWidth * latestUiState.items.size - 1) <= graphWidth) {
                    (graphWidth - (config.maxBarWidth * latestUiState.items.size)) / (latestUiState.items.size - 1)
                } else {
                    config.minSpaceWidth
                }
            }
        }
        val barWidth by remember(config) {
            derivedStateOf {
                if (config.maxBarWidth * latestUiState.items.size + spaceWidth * (latestUiState.items.size - 1) <= graphWidth) {
                    config.maxBarWidth
                } else {
                    (graphWidth - spaceWidth * (latestUiState.items.size - 1)) / latestUiState.items.size
                }
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val pointer = awaitPointerEvent()
                        cursorPosition = pointer.changes.firstOrNull()?.position ?: return@awaitEachGesture
                    }
                },
        ) {
            if (latestUiState.items.isEmpty()) return@Canvas
            if (textMeasureCache.initialized.not()) return@Canvas

            val multilineLabel =
                (spaceWidth + barWidth) <= (textMeasureCache.xLabels.maxOfOrNull { it.size.width } ?: 0).plus(8.dp.toPx())

            val maxLabelHeight = textMeasureCache.xLabels.maxOfOrNull { it.size.height } ?: 0
            val labelBoxHeight = (maxLabelHeight)
                .times(if (multilineLabel) 2 else 1)
                .plus(if (multilineLabel) config.multilineLabelHeightPadding else 0f)
            val graphYHeight = size.height
                .minus(labelBoxHeight)
                .minus(config.graphAndLabelPadding)

            val heightParAmount = graphYHeight / maxTotalValue

            textMeasureCache.xLabels.forEachIndexed { index, item ->
                val y = if (multilineLabel && index % 2 == 1) {
                    maxLabelHeight.toFloat() + config.multilineLabelHeightPadding
                } else {
                    0f
                }.plus(graphYHeight + config.graphAndLabelPadding)

                drawText(
                    textLayoutResult = item,
                    color = contentColor,
                    topLeft = Offset(
                        x = graphBaseX
                            .plus((spaceWidth + barWidth) * index)
                            .plus(barWidth / 2)
                            .minus(item.size.width / 2),
                        y = y,
                    ),
                )
            }
            drawText(
                textLayoutResult = textMeasureCache.minTextMeasureResult,
                color = contentColor,
                topLeft = Offset(0f, graphYHeight - (textMeasureCache.minTextMeasureResult.size.height / 2)),
            )
            drawText(
                textLayoutResult = textMeasureCache.maxTextMeasureResult,
                color = contentColor,
                topLeft = Offset(0f, 0f),
            )
            latestUiState.items.forEachIndexed { index, _ ->
                val x = graphBaseX + (spaceWidth + barWidth).times(index)

                val leftPadding = if (index != 0) spaceWidth / 2f else 0f
                val rightPadding = if (index != latestUiState.items.lastIndex) spaceWidth / 2f else 0f
                val topLeft = Offset(
                    x = x - leftPadding,
                    y = 0f,
                )
                val size = Size(
                    width = barWidth + leftPadding + rightPadding,
                    height = size.height,
                )
                if (Rect(
                        offset = topLeft,
                        size = size,
                    ).contains(cursorPosition)
                ) {
                    drawRect(
                        color = if (index % 2 == 0) Color.Blue else Color.Yellow,
                        topLeft = topLeft,
                        size = size,
                    )
                }
            }
            latestUiState.items.forEachIndexed { index, periodData ->
                val x = graphBaseX + (spaceWidth + barWidth).times(index)
                var beforeY = graphYHeight

                periodData.items.forEach { item ->
                    val itemHeight = (item.value * heightParAmount)
                    drawRect(
                        color = item.color,
                        topLeft = Offset(
                            x = x,
                            y = beforeY - itemHeight,
                        ),
                        size = Size(
                            width = barWidth,
                            height = itemHeight,
                        ),
                    )
                    beforeY -= itemHeight
                }
            }
        }
    }
}

@Stable
private class TextMeasureCache(
    private val textMeasurer: TextMeasurer,
) {
    private var items: ImmutableList<BarGraphUiState.PeriodData> by mutableStateOf(immutableListOf())
    fun updateItems(
        items: ImmutableList<BarGraphUiState.PeriodData>,
    ) {
        this.items = items
    }

    val initialized: Boolean by derivedStateOf { items.isNotEmpty() }

    val minTextMeasureResult by derivedStateOf {
        val minTotalValue = items.minOfOrNull { it.total } ?: 0
        textMeasurer.measure(
            text = AnnotatedString(minTotalValue.toString()),
            constraints = Constraints(),
        )
    }
    val maxTextMeasureResult by derivedStateOf {
        val maxTotalValue = items.maxOfOrNull { it.total } ?: 0
        textMeasurer.measure(
            text = AnnotatedString(maxTotalValue.toString()),
            constraints = Constraints(),
        )
    }

    val xLabels by derivedStateOf {
        items.mapIndexed { index, item ->
            if (index == 0 || item.month == 1) {
                textMeasurer.measure("${item.year}/${item.month}")
            } else {
                textMeasurer.measure("${item.month}")
            }
        }
    }
}

private class Config(
    density: Density,
) {
    val maxBarWidth = with(density) { 42.dp.toPx() }
    val minSpaceWidth = with(density) { 2.dp.toPx() }
    val multilineLabelHeightPadding = with(density) { 4.dp.toPx() }
    val graphAndLabelPadding = with(density) { 8.dp.toPx() }
}
