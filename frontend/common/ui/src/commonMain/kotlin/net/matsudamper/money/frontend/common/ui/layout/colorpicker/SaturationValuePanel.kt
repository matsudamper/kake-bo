package net.matsudamper.money.frontend.common.ui.layout.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.HsvColor

private const val STRIP_COUNT = 64

@Composable
internal fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChanged: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier,
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSaturationValueChanged(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSaturationValueChanged(s, v)
                }
            },
    ) {
        val width = size.width
        val height = size.height
        val stripWidth = width / STRIP_COUNT

        for (i in 0 until STRIP_COUNT) {
            val s = i.toFloat() / (STRIP_COUNT - 1)
            val stripColor = HsvColor(
                hue = hue,
                saturation = s,
                value = 1f,
            ).toColor()

            drawRect(
                color = stripColor,
                topLeft = Offset(i * stripWidth, 0f),
                size = Size(stripWidth + 1f, height),
            )
        }

        val stripHeight = height / STRIP_COUNT
        for (j in 0 until STRIP_COUNT) {
            val alpha = j.toFloat() / (STRIP_COUNT - 1)
            drawRect(
                color = Color.Black.copy(alpha = alpha),
                topLeft = Offset(0f, j * stripHeight),
                size = Size(width, stripHeight + 1f),
            )
        }

        val indicatorX = saturation * width
        val indicatorY = (1f - value) * height
        val indicatorCenter = Offset(indicatorX, indicatorY)
        val indicatorRadius = 8.dp.toPx()

        drawCircle(
            color = Color.White,
            radius = indicatorRadius,
            center = indicatorCenter,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = Color.Black,
            radius = indicatorRadius,
            center = indicatorCenter,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
