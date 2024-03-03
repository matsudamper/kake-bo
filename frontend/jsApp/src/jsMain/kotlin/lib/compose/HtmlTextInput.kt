package lib.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Size
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lib.js.ResizeObserver
import net.matsudamper.money.frontend.common.ui.layout.html.text.input.HtmlTextInputContext
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.minHeight
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.times
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Input

@Composable
internal fun HtmlTextInput(
    id: String,
    widthDensity: Float,
    heightDensity: Float,
    textState: HtmlTextInputContext.TextState,
) {
    val positionInWindow = textState.position

    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(id, textState.sizeCallback) {
        val resizeObserver =
            ResizeObserver { resizeEntries, _ ->
                for (resizeEntry in resizeEntries) {
                    val rect = resizeEntry.contentRect
                    textState.sizeCallback(
                        Size(
                            (rect.width / widthDensity).toFloat(),
                            (rect.height / heightDensity).toFloat(),
                        ),
                    )
                }
            }

        coroutineScope.launch {
            while (isActive) {
                val targetElement = document.getElementById(id)
                if (targetElement == null) {
                    delay(10)
                    continue
                }
                resizeObserver.observe(targetElement)
                break
            }
        }

        onDispose {
            resizeObserver.disconnect()
        }
    }

    Input(
        type = InputType.Text,
        attrs = {
            id(id)
            onChange {
                textState.textCallback(it.value)
            }
            type(InputType.InputTypeWithUnitValue(textState.type ?: "password"))
            inputMode(
                textState.type.toString(),
            )
            placeholder(textState.placeholder)
            style {
                property(
                    "pointer-events",
                    "all",
                )
                val color = textState.color
                if (color != null) {
                    color(rgba(color.red * 255, color.green * 255, color.blue * 255, color.alpha * 255))
                }
                position(Position.Absolute)
                top((positionInWindow.y.px) * heightDensity)
                left((positionInWindow.x.px) * widthDensity)
                minHeight((textState.maxHeight ?: Int.MAX_VALUE).px * heightDensity)
                width(((textState.width ?: Int.MAX_VALUE).px) * widthDensity)
            }
        },
    )
}
