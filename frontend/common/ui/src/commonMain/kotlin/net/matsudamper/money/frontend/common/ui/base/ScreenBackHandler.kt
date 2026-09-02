package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.runtime.Composable

@Composable
public expect fun ScreenBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
