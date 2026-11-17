package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import net.matsudamper.money.frontend.common.base.image.DroppedImages

@Stable
public class ImageDropTargetState internal constructor(
    private val extractor: DroppedImagesExtractor,
    private val onDropImages: State<(DroppedImages) -> Unit>,
    internal val highlightColor: State<Color>,
) {
    public var dragHovering: Boolean by mutableStateOf(false)
        private set

    internal val target: DragAndDropTarget = object : DragAndDropTarget {
        override fun onEntered(event: DragAndDropEvent) {
            dragHovering = true
        }

        override fun onExited(event: DragAndDropEvent) {
            dragHovering = false
        }

        override fun onEnded(event: DragAndDropEvent) {
            dragHovering = false
        }

        override fun onDrop(event: DragAndDropEvent): Boolean {
            dragHovering = false
            val droppedImages = extractor.extract(event) ?: return false
            onDropImages.value(droppedImages)
            return true
        }
    }

    internal fun mayContainImages(event: DragAndDropEvent): Boolean {
        return extractor.mayContainImages(event)
    }
}

@Composable
public fun rememberImageDropTargetState(
    onDropImages: (DroppedImages) -> Unit,
): ImageDropTargetState {
    val extractor = rememberDroppedImagesExtractor()
    val latestOnDropImages = rememberUpdatedState(onDropImages)
    val highlightColor = rememberUpdatedState(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    return remember(extractor) {
        ImageDropTargetState(
            extractor = extractor,
            onDropImages = latestOnDropImages,
            highlightColor = highlightColor,
        )
    }
}

public fun Modifier.imageDropTarget(state: ImageDropTargetState): Modifier {
    return this
        .dragAndDropTarget(
            shouldStartDragAndDrop = { event -> state.mayContainImages(event) },
            target = state.target,
        )
        .drawWithContent {
            drawContent()
            if (state.dragHovering) {
                drawRect(state.highlightColor.value)
            }
        }
}
