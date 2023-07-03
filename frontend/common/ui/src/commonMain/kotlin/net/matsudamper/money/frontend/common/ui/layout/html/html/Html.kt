package net.matsudamper.money.frontend.common.ui.layout.html.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlin.random.Random

@Composable
public fun Html(
    html: String,
    onDismissRequest: () -> Unit,
) {
    val htmlRenderContext = LocalHtmlRenderContext.current
    val id = remember { Random.nextDouble().toString() }
    DisposableEffect(id, html, onDismissRequest) {
        htmlRenderContext.add(
            id = id,
            html = html,
            onDismissRequest = onDismissRequest,
        )
        onDispose {
            htmlRenderContext.remove(id)
        }
    }
}
