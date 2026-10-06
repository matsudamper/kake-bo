package net.matsudamper.money.frontend.common.ui.layout.html.html

import androidx.compose.runtime.Composable

@Composable
public actual fun HtmlText(html: String, onDismissRequest: () -> Unit) {
    Html(html, onDismissRequest)
}
