package net.matsudamper.money.frontend.common.ui.layout.html.html

import androidx.compose.runtime.Composable

@Composable
public expect fun Html(
    html: String,
    onDismissRequest: () -> Unit,
)
