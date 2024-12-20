package net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import kotlin.random.Random
import kotlin.random.nextULong
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType

@Composable
public actual fun FullScreenTextInput(
    title: String,
    onComplete: (String) -> Unit,
    canceled: () -> Unit,
    default: String,
    name: String,
    inputType: TextFieldType,
    isMultiline: Boolean,
) {
    val context = LocalHtmlFullScreenTextInputContext.current
    val id = remember { Random.nextULong().toString() }

    DisposableEffect(Unit) {
        onDispose {
            context.remove(id)
        }
    }
    SideEffect {
        context.set(
            id,
            HtmlFullScreenTextInputContext.TextState(
                title = title,
                default = default,
                inputType = inputType.htmlString(),
                textComplete = {
                    onComplete(it)
                },
                canceled = {
                    canceled()
                },
                isMultiline = isMultiline,
                name = name,
            ),
        )
    }
}
