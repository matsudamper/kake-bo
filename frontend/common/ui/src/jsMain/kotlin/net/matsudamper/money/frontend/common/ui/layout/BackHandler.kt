package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.browser.window
import org.w3c.dom.events.EventListener

@Composable
internal actual fun BackHandler(
    enable: Boolean,
    block: () -> Unit,
) {
    if (enable) {
        var beforeClick: Duration? by remember { mutableStateOf(null) }
        val eventListener =
            remember {
                EventListener {
                    val capturedBeforeClick = beforeClick
                    if (capturedBeforeClick == null ||
                        capturedBeforeClick + 100.milliseconds < window.performance.now().milliseconds
                    ) {
                        beforeClick = window.performance.now().milliseconds
                        block()
                    }
                    window.history.go(1)
                }
            }
        DisposableEffect(eventListener) {
            window.history.pushState(null, "", window.location.href)
            window.addEventListener(
                "popstate",
                callback = eventListener,
            )
            onDispose {
                window.removeEventListener("popstate", eventListener)
                window.history.go(-1)
            }
        }
    }
}
