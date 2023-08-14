package net.matsudamper.money.frontend.common.ui.layout.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList

public data class PolygonalLineGraphItemUiState(
    val year: Int,
    val month: Int,
    val amount: Long,
)

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun PolygonalLineGraph(
    modifier: Modifier = Modifier,
    graphItems: ImmutableList<PolygonalLineGraphItemUiState>,
    contentColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val min = remember(graphItems) {
        graphItems.minOfOrNull { it.amount } ?: 0
    }
    val max = remember(graphItems) {
        graphItems.maxOfOrNull { it.amount } ?: 0
    }
    val textMeasurer = rememberTextMeasurer(cacheSize = 2 + 12)

    Canvas(
        modifier
            .padding(4.dp), // 点がはみ出さないように。計算入れるのが面倒なので全体に追加する
    ) {
        if (graphItems.isEmpty()) return@Canvas

        val amountRange = max - min

        val minTextMeasureResult = textMeasurer.measure(
            text = AnnotatedString(min.toString()),
            constraints = Constraints(),
        )
        val maxTextMeasureResult = textMeasurer.measure(
            text = AnnotatedString(max.toString()),
            constraints = Constraints(),
        )
        val xLabels = graphItems.mapIndexed { index, item ->
            if (index == 0 || item.month == 1) {
                textMeasurer.measure("${item.year}/${item.month}")
            } else {
                textMeasurer.measure("${item.month}")
            }
        }

        val maxYLabelTextWidth = minTextMeasureResult.size.width
            .coerceAtLeast(maxTextMeasureResult.size.width)
            .plus(8.dp.toPx())
        val betweenWidth = (
            (size.width)
                .minus(xLabels.lastOrNull()?.size?.width ?: 0) // 最後のX軸ラベルが表示される分
                .minus(maxYLabelTextWidth) // Y軸ラベルの最大幅
            ).div(graphItems.size - 1)

        val multilineLabel = betweenWidth <= (xLabels.maxOfOrNull { it.size.width } ?: 0).plus(8.dp.toPx())
        val multilineLabelHeightPadding = 4.dp.toPx()
        val maxLabelHeight = xLabels.maxOfOrNull { it.size.height } ?: 0
        val labelBoxHeight = (maxLabelHeight)
            .times(if (multilineLabel) 2 else 1)
            .plus(if (multilineLabel) multilineLabelHeightPadding else 0f)

        val graphAndLabelPadding = 8.dp.toPx()
        val graphYHeight = size.height
            .minus(labelBoxHeight)
            .minus(graphAndLabelPadding)

        val heightParAmount = graphYHeight / amountRange

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
                    x = maxYLabelTextWidth + betweenWidth * index,
                    y = y,
                ),
            )
        }

        if (amountRange == 0.toLong()) {
            drawText(
                textLayoutResult = minTextMeasureResult,
                color = contentColor,
                topLeft = Offset(0f, size.height / 2 - (maxTextMeasureResult.size.height / 2)),
            )
            drawLine(
                color = contentColor,
                strokeWidth = 2.dp.toPx(),
                start = Offset(
                    x = maxYLabelTextWidth,
                    y = graphYHeight / 2,
                ),
                end = Offset(
                    x = size.width,
                    y = graphYHeight / 2,
                ),
            )
        } else {
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
            graphItems.forEachIndexed { index, item ->
                drawCircle(
                    color = contentColor,
                    radius = 4.dp.toPx(),
                    center = Offset(
                        x = maxYLabelTextWidth + betweenWidth * index,
                        y = graphYHeight - (item.amount - min) * heightParAmount,
                    ),
                )
            }
            graphItems.zipWithNext().forEachIndexed { index, graphItem ->
                val current = graphItem.first
                val next = graphItem.second
                drawLine(
                    color = contentColor,
                    strokeWidth = 2.dp.toPx(),
                    start = Offset(
                        x = maxYLabelTextWidth + betweenWidth * index,
                        y = graphYHeight - (current.amount - min) * heightParAmount,
                    ),
                    end = Offset(
                        x = maxYLabelTextWidth + betweenWidth * (index + 1),
                        y = graphYHeight - (next.amount - min) * heightParAmount,
                    ),
                )
            }
        }
    }
}
