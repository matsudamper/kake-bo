package net.matsudamper.money.frontend.common.ui.layout.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val SEGMENT_COUNT = 72
private const val SEGMENT_DEGREES = 360f / SEGMENT_COUNT

@Composable
internal fun HueWheel(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier,
) {
    var canvasCenter by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onHueChanged(positionToHue(offset, canvasCenter))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onHueChanged(positionToHue(change.position, canvasCenter))
                }
            },
    ) {
        canvasCenter = Offset(size.width / 2f, size.height / 2f)
        val ringThickness = 28.dp.toPx()
        val outerRadius = size.minDimension / 2f
        val arcSize = Size(
            outerRadius * 2f - ringThickness,
            outerRadius * 2f - ringThickness,
        )
        val arcTopLeft = Offset(
            canvasCenter.x - arcSize.width / 2f,
            canvasCenter.y - arcSize.height / 2f,
        )

        for (i in 0 until SEGMENT_COUNT) {
            val segmentHue = i * SEGMENT_DEGREES
            val segmentColor = HsvColor(
                hue = segmentHue,
                saturation = 1f,
                value = 1f,
            ).toColor()

            drawArc(
                color = segmentColor,
                startAngle = segmentHue - 90f,
                sweepAngle = SEGMENT_DEGREES + 0.5f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringThickness),
            )
        }

        val indicatorRadius = ringThickness / 2f - 2.dp.toPx()
        val ringCenterRadius = outerRadius - ringThickness / 2f
        val angleRad = (hue - 90f) * PI.toFloat() / 180f
        val indicatorCenter = Offset(
            canvasCenter.x + ringCenterRadius * cos(angleRad),
            canvasCenter.y + ringCenterRadius * sin(angleRad),
        )

        drawCircle(
            color = Color.White,
            radius = indicatorRadius,
            center = indicatorCenter,
        )
        drawCircle(
            color = Color.DarkGray,
            radius = indicatorRadius,
            center = indicatorCenter,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private fun positionToHue(position: Offset, center: Offset): Float {
    val dx = position.x - center.x
    val dy = position.y - center.y
    val angleDegrees = atan2(dy, dx) * 180f / PI.toFloat()
    return ((angleDegrees + 90f) % 360f + 360f) % 360f
}
