package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

public actual fun Modifier.calendarHorizontalSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier = this.pointerInput(onSwipeLeft, onSwipeRight) {
    var totalDragAmount = 0f
    detectHorizontalDragGestures(
        onDragStart = {
            totalDragAmount = 0f
        },
        onDragEnd = {
            val threshold = size.width * 0.15f
            if (totalDragAmount > threshold) {
                onSwipeRight()
            } else if (totalDragAmount < -threshold) {
                onSwipeLeft()
            }
        },
        onDragCancel = {
            totalDragAmount = 0f
        },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            totalDragAmount += dragAmount
        },
    )
}
