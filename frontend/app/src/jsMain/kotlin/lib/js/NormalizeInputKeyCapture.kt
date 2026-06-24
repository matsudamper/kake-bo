package lib.js

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { focusRequester.freeFocus() }
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
        val shadowRoot = divs.item(index)?.asDynamic()?.shadowRoot
        if (shadowRoot != null) {
            val canvas = shadowRoot.querySelector("canvas")
            if (canvas != null) {
                return canvas.unsafeCast<HTMLCanvasElement>()
            }
        }
    }
    return null
}
