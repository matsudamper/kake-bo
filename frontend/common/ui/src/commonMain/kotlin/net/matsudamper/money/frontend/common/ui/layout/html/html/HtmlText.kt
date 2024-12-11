package net.matsudamper.money.frontend.common.ui.layout.html.html

import androidx.compose.runtime.Composable

@Composable
public expect fun HtmlText(
    html: String,
    onDismissRequest: () -> Unit,
)
