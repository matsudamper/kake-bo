package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.image.DroppedImages
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_upload
import org.jetbrains.compose.resources.painterResource

@Composable
public fun ImageUploadDropArea(
    onClickUpload: () -> Unit,
    onDropImages: (DroppedImages) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extractor = rememberDroppedImagesExtractor()
    val latestOnDropImages by rememberUpdatedState(onDropImages)
    var dragHovering by remember { mutableStateOf(false) }
    val dropTarget = remember(extractor) {
        object : DragAndDropTarget {
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
                latestOnDropImages(droppedImages)
                return true
            }
        }
    }
    ImageUploadDropAreaContent(
        modifier = modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { event -> extractor.mayContainImages(event) },
            target = dropTarget,
        ),
        dragHovering = dragHovering,
        onClickUpload = onClickUpload,
    )
}

@Composable
private fun ImageUploadDropAreaContent(
    dragHovering: Boolean,
    onClickUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (dragHovering) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val backgroundColor = if (dragHovering) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, shape)
            .drawBehind {
                val strokeWidth = 1.5.dp.toPx()
                val inset = strokeWidth / 2
                drawRoundRect(
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    color = borderColor,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = if (dragHovering) {
                            null
                        } else {
                            PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                        },
                    ),
                )
            }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_upload),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = borderColor,
        )
        Text(
            text = if (dragHovering) "ドロップしてアップロード" else "ここに画像をドロップ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        ImageUploadButton(
            onClick = onClickUpload,
        )
    }
}

@Composable
@Preview
private fun ImageUploadDropAreaPreview() {
    AppRoot(isDarkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ImageUploadDropAreaContent(
                dragHovering = false,
                onClickUpload = {},
            )
            ImageUploadDropAreaContent(
                dragHovering = true,
                onClickUpload = {},
            )
        }
    }
}
