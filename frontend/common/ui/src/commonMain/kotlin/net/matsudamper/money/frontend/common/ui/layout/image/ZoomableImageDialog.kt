package net.matsudamper.money.frontend.common.ui.layout.image

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.ImmutableList

@Immutable
public data class ZoomableImageDialogImages(
    val imageUrls: ImmutableList<String>,
    val initialIndex: Int,
)

@Composable
public expect fun ZoomableImageDialog(
    images: ZoomableImageDialogImages,
    onDismissRequest: () -> Unit,
)

@Composable
internal fun ZoomableImagePager(
    images: ZoomableImageDialogImages,
    modifier: Modifier = Modifier,
    pageContent: @Composable (imageUrl: String) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = images.initialIndex.coerceIn(0, (images.imageUrls.size - 1).coerceAtLeast(0)),
        pageCount = { images.imageUrls.size },
    )
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                movePageByArrowKey(
                    event = event,
                    pagerState = pagerState,
                    coroutineScope = coroutineScope,
                )
            },
    ) {
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
        ) { page ->
            pageContent(images.imageUrls[page])
        }
        if (images.imageUrls.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    text = "${pagerState.currentPage + 1} / ${images.imageUrls.size}",
                )
            }
        }
    }
}

private fun movePageByArrowKey(
    event: KeyEvent,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
): Boolean {
    val targetPage = when {
        event.type != KeyEventType.KeyDown -> null
        event.key == Key.DirectionLeft -> pagerState.currentPage - 1
        event.key == Key.DirectionRight -> pagerState.currentPage + 1
        else -> null
    }?.takeIf { it in 0 until pagerState.pageCount }

    return if (targetPage == null) {
        false
    } else {
        coroutineScope.launch {
            pagerState.animateScrollToPage(targetPage)
        }
        true
    }
}
