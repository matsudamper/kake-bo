package net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.random.Random
import kotlin.random.nextULong

@Composable
public fun HtmlFullScreenTextInput(
    title: String,
    onComplete: (String) -> Unit,
    canceled: () -> Unit,
    default: String,
) {
    val context = LocalHtmlFullScreenTextInputContext.current
    val id = remember { Random.nextULong().toString() }

    DisposableEffect(Unit) {
        onDispose {
            context.remove(id)
        }
    }
    LaunchedEffect(Unit) {
        context.set(
            id,
            HtmlFullScreenTextInputContext.TextState(
                title = title,
                default = default,
                textComplete = {
                    onComplete(it)
                },
                canceled = {
                    canceled()
                },
            ),
        )
    }
}
