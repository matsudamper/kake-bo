package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

private const val SWIPE_THRESHOLD_PX = 100f

@Composable
public fun HorizontalSwipeDetector(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var totalDrag by remember { mutableStateOf(0f) }
    Box(
        modifier = modifier.pointerInput(onSwipeLeft, onSwipeRight) {
            detectHorizontalDragGestures(
                onDragStart = {
                    totalDrag = 0f
                },
                onDragEnd = {
                    if (totalDrag > SWIPE_THRESHOLD_PX) {
                        onSwipeRight()
                    } else if (totalDrag < -SWIPE_THRESHOLD_PX) {
                        onSwipeLeft()
                    }
                    totalDrag = 0f
                },
                onDragCancel = {
                    totalDrag = 0f
                },
                onHorizontalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                },
            )
        },
    ) {
        content()
    }
}
