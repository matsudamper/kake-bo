package net.matsudamper.money.frontend.common.ui.layout.html.html

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml

@Composable
public actual fun HtmlText(html: String, onDismissRequest: () -> Unit) {
    SelectionContainer {
        Text(text = AnnotatedString.fromHtml(html))
    }
}
