package lib.compose

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.Button
import org.w3c.dom.HTMLButtonElement

@Composable
internal fun ConfirmButton(
    attrs: AttrBuilderContext<HTMLButtonElement>? = null,
    content: @Composable () -> Unit,
) {
    Button(
        attrs = {
            attrs?.invoke(this)
            style {
                borderRadius(0.2.em)
                padding(0.3.em, 0.6.em)
                fontSize(1.1.em)
                backgroundColor(Color.white)
            }
        },
    ) {
        content()
    }
}
