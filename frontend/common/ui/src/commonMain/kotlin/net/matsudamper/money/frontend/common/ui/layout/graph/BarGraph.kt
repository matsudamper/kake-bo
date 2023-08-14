package net.matsudamper.money.frontend.common.ui.layout.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList

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

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun BarGraph(
    modifier: Modifier = Modifier,
    uiState: BarGraphUiState,
    contentColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val minTotalValue = remember(uiState.items) {
        uiState.items.minOfOrNull { it.total } ?: 0
    }
    val maxTotalValue = remember(uiState.items) {
        uiState.items.maxOfOrNull { it.total } ?: 0
    }
    val textMeasurer = rememberTextMeasurer(cacheSize = 2 + 12)

    val minTextMeasureResult = remember(minTotalValue) {
        textMeasurer.measure(
            text = AnnotatedString(minTotalValue.toString()),
            constraints = Constraints(),
        )
    }
    val maxTextMeasureResult = remember(maxTotalValue) {
        textMeasurer.measure(
            text = AnnotatedString(maxTotalValue.toString()),
            constraints = Constraints(),
        )
    }
    Row(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (uiState.items.isEmpty()) return@Canvas

            val maxBarWidth = 42.dp.toPx()
            val minSpaceWidth = 2.dp.toPx()

            val xLabels = uiState.items.mapIndexed { index, item ->
                if (index == 0 || item.month == 1) {
                    textMeasurer.measure("${item.year}/${item.month}")
                } else {
                    textMeasurer.measure("${item.month}")
                }
            }

            val maxYLabelTextWidth = minTextMeasureResult.size.width
                .coerceAtLeast(maxTextMeasureResult.size.width)
                .plus(8.dp.toPx())

            val graphWidth = size.width - maxYLabelTextWidth
            val graphBaseX = maxYLabelTextWidth

            val spaceWidth: Float =
                if ((maxBarWidth * uiState.items.size) + (minSpaceWidth * uiState.items.size - 1) <= graphWidth) {
                    (graphWidth - (maxBarWidth * uiState.items.size)) / (uiState.items.size - 1)
                } else {
                    minSpaceWidth
                }
            val barWidth = if (maxBarWidth * uiState.items.size + spaceWidth * (uiState.items.size - 1) <= graphWidth) {
                maxBarWidth
            } else {
                (graphWidth - spaceWidth * (uiState.items.size - 1)) / uiState.items.size
            }

            val multilineLabel =
                (spaceWidth + barWidth) <= (xLabels.maxOfOrNull { it.size.width } ?: 0).plus(8.dp.toPx())
            val multilineLabelHeightPadding = 4.dp.toPx()
            val maxLabelHeight = xLabels.maxOfOrNull { it.size.height } ?: 0
            val labelBoxHeight = (maxLabelHeight)
                .times(if (multilineLabel) 2 else 1)
                .plus(if (multilineLabel) multilineLabelHeightPadding else 0f)

            val graphAndLabelPadding = 8.dp.toPx()
            val graphYHeight = size.height
                .minus(labelBoxHeight)
                .minus(graphAndLabelPadding)

            val heightParAmount = graphYHeight / maxTotalValue

            xLabels.forEachIndexed { index, item ->
                val y = if (multilineLabel && index % 2 == 1) {
                    maxLabelHeight.toFloat() + multilineLabelHeightPadding
                } else {
                    0f
                }.plus(graphYHeight + graphAndLabelPadding)

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
                textLayoutResult = minTextMeasureResult,
                color = contentColor,
                topLeft = Offset(0f, graphYHeight - (minTextMeasureResult.size.height / 2)),
            )
            drawText(
                textLayoutResult = maxTextMeasureResult,
                color = contentColor,
                topLeft = Offset(0f, 0f),
            )
            uiState.items.forEachIndexed { index, periodData ->
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
