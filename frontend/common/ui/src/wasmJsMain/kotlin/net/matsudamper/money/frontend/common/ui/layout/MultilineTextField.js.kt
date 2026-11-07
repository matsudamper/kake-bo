package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
public actual fun MultilineTextField(
    text: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = colors.outline,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        HtmlElementView(
            modifier = Modifier
                .fillMaxWidth()
                .height(TextAreaHeight),
            factory = {
                document.createElement("textarea") as HTMLTextAreaElement
            },
            update = { textArea ->
                textArea.placeholder = placeholder
                applyTextAreaStyle(
                    textArea = textArea,
                    fontSizeSp = textStyle.fontSize.value,
                    textColor = colors.onSurface,
                )
                if (textArea.value != text) {
                    textArea.value = text
                }
                textArea.oninput = {
                    if (textArea.value != text) {
                        onValueChange(textArea.value)
                    }
                }
            },
        )
    }
}

private val TextAreaHeight = 120.dp

private fun applyTextAreaStyle(
    textArea: HTMLTextAreaElement,
    fontSizeSp: Float,
    textColor: Color,
) {
    textArea.style.apply {
        boxSizing = "border-box"
        display = "block"
        width = "100%"
        height = "100%"
        margin = "0"
        padding = "0"
        border = "none"
        outline = "none"
        backgroundColor = "transparent"
        color = textColor.toCssRgb()
        fontSize = "${fontSizeSp}px"
        fontFamily = "inherit"
        setProperty("resize", "none")
    }
}

private fun Color.toCssRgb(): String {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "rgb($r, $g, $b)"
}
