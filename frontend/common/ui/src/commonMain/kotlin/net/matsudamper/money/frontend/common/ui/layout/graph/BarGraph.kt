package net.matsudamper.money.frontend.common.ui.layout.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf

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

    BoxWithConstraints(modifier = modifier) {
        var cursorPosition by remember { mutableStateOf(Offset.Zero) }
        val measureState = remember(density, config) {
            GraphMeasureState(
                density = density,
                config = config,
                textMeasureCache = textMeasureCache,
            )
        }
        LaunchedEffect(maxWidth, maxHeight) {
            measureState.update(
                width = maxWidth,
                height = maxHeight,
            )
        }
        LaunchedEffect(latestUiState.items.size) {
            measureState.size(latestUiState.items.size)
        }
        val measureResult = measureState.measure()
        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val pointer = awaitPointerEvent()
                        cursorPosition = pointer.changes.firstOrNull()?.position ?: return@awaitEachGesture
                    }
                }
                .pointerInput(measureResult.graphRangeRects) {
                    awaitEachGesture {
                        val pointer = awaitPointerEvent()

                        when (pointer.type) {
                            PointerEventType.Press -> {
                                pointer.changes.forEach { it.consume() }
                                while (true) {
                                    val releasePointer = awaitPointerEvent()
                                    when (releasePointer.type) {
                                        PointerEventType.Release -> {
                                            for (change in releasePointer.changes) {
                                                val clickedIndex = measureResult.graphRangeRects.indexOfFirst { it.contains(change.position) }
                                                    .takeIf { it >= 0 }
                                                    ?: continue
                                                latestUiState.items.getOrNull(clickedIndex)?.event?.onClick()
                                                break
                                            }
                                            break
                                        }

                                        PointerEventType.Exit -> break
                                    }
                                }
                            }

                            else -> Unit
                        }
                    }
                },
        ) {
            if (latestUiState.items.isEmpty()) return@Canvas
            if (textMeasureCache.initialized.not()) return@Canvas

            val multilineLabel = (measureResult.spaceWidth + measureResult.barWidth) <= (textMeasureCache.xLabels.maxOfOrNull { it.size.width } ?: 0).plus(8.dp.toPx())

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
                        x = measureResult.graphBaseX
                            .plus((measureResult.spaceWidth + measureResult.barWidth) * index)
                            .plus(measureResult.barWidth / 2)
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
            latestUiState.items.forEachIndexed { index, periodData ->
                val x = measureResult.graphBaseX + (measureResult.spaceWidth + measureResult.barWidth).times(index)
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
                            width = measureResult.barWidth,
                            height = itemHeight,
                        ),
                    )
                    beforeY -= itemHeight
                }
            }

            measureResult.graphRangeRects.forEach { rect ->
                if (rect.contains(cursorPosition)) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.1f),
                        topLeft = rect.topLeft,
                        size = rect.size,
                    )
                }
            }
        }
    }
}

private class GraphMeasureState(
    val density: Density,
    val config: Config,
    textMeasureCache: TextMeasureCache,
) {
    fun update(width: Dp, height: Dp) {
        graphWidth = with(density) { width.toPx() }
        graphHeight = with(density) { height.toPx() }
    }

    private var itemSize by mutableStateOf(0)
    fun size(size: Int) {
        this.itemSize = size
    }

    private var graphWidth: Float by mutableFloatStateOf(0f)
    private var graphHeight: Float by mutableFloatStateOf(0f)

    private val yLabelAndPaddingWidth by derivedStateOf {
        textMeasureCache.yLabelMaxWidth
            .plus(with(density) { 8.dp.toPx() })
    }
    val graphBaseX by derivedStateOf { yLabelAndPaddingWidth }

    fun measure(): MeasureResult {
        val graphWidth = graphWidth - yLabelAndPaddingWidth
        val spaceWidth = if ((config.maxBarWidth * itemSize) + (config.minSpaceWidth * itemSize - 1) <= graphWidth) {
            (graphWidth - (config.maxBarWidth * itemSize)) / (itemSize - 1)
        } else {
            config.minSpaceWidth
        }

        val barWidth = if (config.maxBarWidth * itemSize + spaceWidth * (itemSize - 1) <= graphWidth) {
            config.maxBarWidth
        } else {
            (graphWidth - spaceWidth * (itemSize - 1)) / itemSize
        }

        val graphRangeRects = (0 until itemSize).map { index ->
            val x = graphBaseX + (spaceWidth + barWidth).times(index)

            val leftPadding = if (index != 0) spaceWidth / 2f else 0f
            val rightPadding = if (index != (itemSize - 1)) spaceWidth / 2f else 0f
            val topLeft = Offset(
                x = x - leftPadding,
                y = 0f,
            )
            val size = Size(
                width = barWidth + leftPadding + rightPadding,
                height = graphHeight,
            )
            Rect(
                offset = topLeft,
                size = size,
            )
        }.toImmutableList()

        return MeasureResult(
            graphBaseX = graphBaseX,
            spaceWidth = spaceWidth,
            barWidth = barWidth,
            graphRangeRects = graphRangeRects,
        )
    }

    data class MeasureResult(
        val graphBaseX: Float,
        val spaceWidth: Float,
        val barWidth: Float,
        val graphRangeRects: ImmutableList<Rect>,
    )
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

    val yLabelMaxWidth by derivedStateOf {
        minTextMeasureResult.size.width
            .coerceAtLeast(maxTextMeasureResult.size.width)
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
