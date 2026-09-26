package lib.js

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.KeyboardEvent

@Composable
public fun NormalizeInputKeyCapture(content: @Composable () -> Unit) {
    var hasFocus by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        val target = awaitComposeCanvas()
        target.addEventListener(
            type = "keydown",
            callback = { event ->
                event as KeyboardEvent

                if (hasFocus) {
                    event.stopImmediatePropagation()
                }
            },
        )
    }

    val focusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .focusTarget()
            .focusRequester(focusRequester)
            // clickable は子孫のセマンティクスをマージするため、paneTitle を持つ子孫(Snackbar 等)があると Web の a11y 同期で例外になる
            .pointerInput(focusRequester) {
                detectTapGestures { focusRequester.freeFocus() }
            }
            .onFocusChanged {
                hasFocus = it.hasFocus
            },
    ) {
        content()
    }
}

// ComposeViewportはviewportContainer配下のShadow DOM内にidの無いcanvasを生成するため、走査して取得する
private suspend fun awaitComposeCanvas(): HTMLCanvasElement {
    while (true) {
        val canvas = findComposeCanvas()
        if (canvas != null) {
            return canvas
        }
        delay(50)
    }
}

private fun findComposeCanvas(): HTMLCanvasElement? {
    val container = document.getElementById("ComposeTargetContainer") ?: return null
    val divs = container.getElementsByTagName("div")
    for (index in 0 until divs.length) {
        val canvas = divs.item(index)?.shadowRoot?.querySelector("canvas") as? HTMLCanvasElement
        if (canvas != null) {
            return canvas
        }
    }
    return null
}
