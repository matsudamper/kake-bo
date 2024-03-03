package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.runtime.Composable

@Composable
internal expect fun BackHandler(
    enable: Boolean,
    block: () -> Unit,
)
