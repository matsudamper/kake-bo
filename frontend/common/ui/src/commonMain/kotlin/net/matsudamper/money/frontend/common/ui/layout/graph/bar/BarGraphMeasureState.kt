package net.matsudamper.money.frontend.common.ui.layout.graph.bar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

internal class BarGraphMeasureState(
    val density: Density,
    val config: BarGraphConfig,
    textMeasureCache: BarGraphTextMeasureCache,
) {
    fun update(constraints: Constraints) {
        this.containerConstraints = constraints
    }

    fun size(size: Int) {
        this.itemSize = size
    }

    private var itemSize by mutableStateOf(0)

    private var containerConstraints: Constraints by mutableStateOf(Constraints())
    val containerWidth: Float by derivedStateOf {
        val minWidth =
            when (itemSize) {
                0,
                1,
                -> config.maxBarWidth + config.defaultSpaceWidth

                else -> {
                    (config.maxBarWidth * itemSize) + (config.defaultSpaceWidth * (itemSize - 1)) + yLabelAndPaddingWidth
                }
            }
        (containerConstraints.minWidth.toFloat())
            .coerceAtLeast(minimumValue = minWidth)
            .coerceAtMost(maximumValue = containerConstraints.maxWidth.toFloat())
    }
    private val graphWidth: Float by derivedStateOf { containerWidth - yLabelAndPaddingWidth }
    private val containerHeight: Float by derivedStateOf { containerConstraints.maxHeight.toFloat() }

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

    /**
     * まだスナップショットが作成されていないのでエラーになるので、コード上すぐに必要な値を遅延して取得できるようにする
     * Reading a state that was created after the snapsho…en or in a snapshot that has not yet been applied
     */
    @Stable
    @Composable
    fun getGraphRangeRectsKey(): Any? {
        return remember { snapshotFlow { graphRangeRects } }.collectAsState(null).value
    }

    val graphRangeRects by derivedStateOf {
        (0 until itemSize).map { index ->
            val x = graphBaseX + (spaceWidth + barWidth).times(index)

            val leftPadding = if (index != 0) spaceWidth / 2f else 0f
            val rightPadding = if (index != (itemSize - 1)) spaceWidth / 2f else 0f
            val topLeft =
                Offset(
                    x = x - leftPadding,
                    y = 0f,
                )
            val size =
                Size(
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
