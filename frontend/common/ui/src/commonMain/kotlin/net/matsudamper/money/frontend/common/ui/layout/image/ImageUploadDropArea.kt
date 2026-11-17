package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_upload
import org.jetbrains.compose.resources.painterResource

/**
 * ドロップ自体は [imageDropTarget] を付けた親が受け付ける。ここはドロップ先の目印とアップロードボタンを表示する
 */
@Composable
public fun ImageUploadDropArea(
    dragHovering: Boolean,
    onClickUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (dragHovering) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
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
            ImageUploadDropArea(
                dragHovering = false,
                onClickUpload = {},
            )
            ImageUploadDropArea(
                dragHovering = true,
                onClickUpload = {},
            )
        }
    }
}
