package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog

@Composable
public fun MoneyUsageImageThumbnail(
    url: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClickReplace: () -> Unit,
    onClickDelete: () -> Unit,
) {
    var showPopupMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { showPopupMenu = true },
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val pressEvent = awaitPointerEvent()
                        if (pressEvent.type != PointerEventType.Press) return@awaitEachGesture
                        if (pressEvent.buttons.isSecondaryPressed.not()) return@awaitEachGesture

                        while (true) {
                            val releaseEvent = awaitPointerEvent()
                            if (releaseEvent.type != PointerEventType.Release) continue
                            showPopupMenu = true
                            break
                        }
                    }
                },
            loading = { ImageLoadingPlaceholder() },
        )
        DropdownMenu(
            expanded = showPopupMenu,
            onDismissRequest = { showPopupMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("入れ替え") },
                onClick = {
                    showPopupMenu = false
                    onClickReplace()
                },
            )
            DropdownMenuItem(
                text = { Text("削除") },
                onClick = {
                    showPopupMenu = false
                    showDeleteDialog = true
                },
            )
        }
        if (showDeleteDialog) {
            AlertDialog(
                title = { Text("画像を削除しますか？") },
                description = { Text("この操作は取り消せません。") },
                positiveButton = { Text("削除") },
                negativeButton = { Text("キャンセル") },
                onClickPositive = {
                    showDeleteDialog = false
                    onClickDelete()
                },
                onClickNegative = { showDeleteDialog = false },
                onDismissRequest = { showDeleteDialog = false },
            )
        }
    }
}

@Composable
@Preview
private fun MoneyUsageImageThumbnailMenuPreview() {
    AppRoot(isDarkTheme = false) {
        Box(modifier = Modifier.size(120.dp)) {
            SubcomposeAsyncImage(
                model = "https://picsum.photos/seed/kakebo-preview/240/240",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ImageLoadingPlaceholder() },
            )
            DropdownMenu(
                expanded = true,
                onDismissRequest = {},
            ) {
                DropdownMenuItem(
                    text = { Text("入れ替え") },
                    onClick = {},
                )
                DropdownMenuItem(
                    text = { Text("削除") },
                    onClick = {},
                )
            }
        }
    }
}
