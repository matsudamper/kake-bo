package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.layout.AlertDialog

public data class MoneyUsageImageThumbnailUiState(
    val url: String,
    val isReplacing: Boolean,
    val event: Event,
) {
    @Immutable
    public interface Event {
        public fun onClick()

        public fun onClickReplace()

        public fun onClickDelete()
    }
}

@Composable
public fun MoneyUsageImageThumbnail(
    uiState: MoneyUsageImageThumbnailUiState,
    modifier: Modifier = Modifier,
) {
    var showPopupMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        SubcomposeAsyncImage(
            model = uiState.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isReplacing) {
                    if (uiState.isReplacing) return@pointerInput
                    awaitEachGesture {
                        // 右クリックをcombinedClickableに渡すと通常タップとして画像拡大が開くため、Initialで消費する
                        val pressEvent = awaitPointerEvent(PointerEventPass.Initial)
                        if (pressEvent.type != PointerEventType.Press) return@awaitEachGesture
                        if (pressEvent.buttons.isSecondaryPressed.not()) return@awaitEachGesture
                        pressEvent.changes.forEach { it.consume() }

                        while (true) {
                            val releaseEvent = awaitPointerEvent(PointerEventPass.Initial)
                            releaseEvent.changes.forEach { it.consume() }
                            if (releaseEvent.type != PointerEventType.Release) continue
                            showPopupMenu = true
                            break
                        }
                    }
                }
                .combinedClickable(
                    enabled = uiState.isReplacing.not(),
                    onClick = { uiState.event.onClick() },
                    onLongClickLabel = "画像のメニューを開く",
                    onLongClick = { showPopupMenu = true },
                ),
            loading = { ImageLoadingPlaceholder() },
        )
        if (uiState.isReplacing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        DropdownMenu(
            expanded = showPopupMenu,
            onDismissRequest = { showPopupMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("入れ替え") },
                onClick = {
                    showPopupMenu = false
                    uiState.event.onClickReplace()
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
                    uiState.event.onClickDelete()
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
