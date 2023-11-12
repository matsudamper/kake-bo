package net.matsudamper.money.frontend.common.ui.layout.graph.bar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
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
    val latestUiState by rememberUpdatedState(uiState)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer(cacheSize = 2 + 12)
    val config = remember { BarGraphConfig(density) }
    val maxTotalValue by remember { derivedStateOf { latestUiState.items.maxOfOrNull { it.total } ?: 0 } }
    val textMeasureCache = remember(textMeasurer) {
        BarGraphTextMeasureCache(
            textMeasurer = textMeasurer,
        )
    }
    LaunchedEffect(latestUiState.items) {
        textMeasureCache.updateItems(latestUiState.items)
    }

    BoxWithConstraints(modifier = modifier) {
        var cursorPosition by remember { mutableStateOf(Offset.Zero) }
        val measureState = remember(density, config) {
            BarGraphMeasureState(
                density = density,
                config = config,
                textMeasureCache = textMeasureCache,
            )
        }
        LaunchedEffect(constraints) {
            measureState.update(
                constraints = constraints,
            )
        }
        LaunchedEffect(latestUiState.items.size) {
            measureState.size(latestUiState.items.size)
        }
        Canvas(
            modifier = Modifier.fillMaxHeight()
                .width(with(density) { measureState.containerWidth.toDp() })
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val pointer = awaitPointerEvent()
                        cursorPosition = pointer.changes.firstOrNull()?.position ?: return@awaitEachGesture
                    }
                }
                .pointerInput(measureState.graphRangeRects) {
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
                                                val clickedIndex = measureState.graphRangeRects.indexOfFirst { it.contains(change.position) }
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

            val multilineLabel = (measureState.spaceWidth + measureState.barWidth) <= (textMeasureCache.xLabels.maxOfOrNull { it.size.width } ?: 0).plus(8.dp.toPx())

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
                        x = measureState.graphBaseX
                            .plus((measureState.spaceWidth + measureState.barWidth) * index)
                            .plus(measureState.barWidth / 2)
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
                val x = measureState.graphBaseX + (measureState.spaceWidth + measureState.barWidth).times(index)
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
                            width = measureState.barWidth,
                            height = itemHeight,
                        ),
                    )
                    beforeY -= itemHeight
                }
            }

            measureState.graphRangeRects.forEach { rect ->
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
