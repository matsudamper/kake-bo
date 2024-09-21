package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.runtime.Composable

@Composable
public expect fun BackHandler(
    enable: Boolean,
    block: () -> Unit,
)
