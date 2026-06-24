package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
public actual fun ZoomableImageDialog(
    imageUrl: String,
    onDismissRequest: () -> Unit,
) {
    val zoomState = rememberZoomState()
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window

    SideEffect {
        dialogWindow?.setDimAmount(0f)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val painter = rememberAsyncImagePainter(imageUrl)
            when (val state = painter.state.collectAsState().value) {
                is AsyncImagePainter.State.Empty,
                is AsyncImagePainter.State.Error,
                is AsyncImagePainter.State.Loading,
                -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is AsyncImagePainter.State.Success -> {
                    LaunchedEffect(state.painter.intrinsicSize) {
                        zoomState.setContentSize(state.painter.intrinsicSize)
                    }
                    Image(
                        modifier = Modifier
                            .zoomable(zoomState),
                        painter = state.painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}
