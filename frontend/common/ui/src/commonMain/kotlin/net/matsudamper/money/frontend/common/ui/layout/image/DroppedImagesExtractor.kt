package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.draganddrop.DragAndDropEvent
import net.matsudamper.money.frontend.common.base.image.DroppedImages

internal interface DroppedImagesExtractor {
    fun mayContainImages(event: DragAndDropEvent): Boolean

    fun extract(event: DragAndDropEvent): DroppedImages?
}

@Composable
internal expect fun rememberDroppedImagesExtractor(): DroppedImagesExtractor
