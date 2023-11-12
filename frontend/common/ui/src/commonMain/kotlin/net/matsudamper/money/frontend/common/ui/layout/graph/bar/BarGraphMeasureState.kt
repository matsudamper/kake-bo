package net.matsudamper.money.frontend.common.ui.layout.graph.bar

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal class BarGraphMeasureState(
    val density: Density,
    val config: BarGraphConfig,
    textMeasureCache: BarGraphTextMeasureCache,
) {
    fun update(width: Dp, height: Dp) {
        containerWidth = with(density) { width.toPx() }
        containerHeight = with(density) { height.toPx() }
    }

    fun size(size: Int) {
        this.itemSize = size
    }

    private var itemSize by mutableStateOf(0)

    private var containerWidth: Float by mutableFloatStateOf(0f)
    private val graphWidth: Float by derivedStateOf { containerWidth - yLabelAndPaddingWidth }
    private var containerHeight: Float by mutableFloatStateOf(0f)

    private val yLabelAndPaddingWidth by derivedStateOf {
        textMeasureCache.yLabelMaxWidth
            .plus(with(density) { 8.dp.toPx() })
    }
    val graphBaseX by derivedStateOf { yLabelAndPaddingWidth }

    val spaceWidth by derivedStateOf {
        if ((config.maxBarWidth * itemSize) + (config.minSpaceWidth * itemSize - 1) <= graphWidth) {
            (graphWidth - (config.maxBarWidth * itemSize)) / (itemSize - 1)
        } else {
            config.minSpaceWidth
        }
    }

    val barWidth by derivedStateOf {
        if (config.maxBarWidth * itemSize + spaceWidth * (itemSize - 1) <= graphWidth) {
            config.maxBarWidth
        } else {
            (graphWidth - spaceWidth * (itemSize - 1)) / itemSize
        }
    }

    val graphRangeRects by derivedStateOf {
        (0 until itemSize).map { index ->
            val x = graphBaseX + (spaceWidth + barWidth).times(index)

            val leftPadding = if (index != 0) spaceWidth / 2f else 0f
            val rightPadding = if (index != (itemSize - 1)) spaceWidth / 2f else 0f
            val topLeft = Offset(
                x = x - leftPadding,
                y = 0f,
            )
            val size = Size(
                width = barWidth + leftPadding + rightPadding,
                height = containerHeight,
            )
            Rect(
                offset = topLeft,
                size = size,
            )
        }
    }
}
