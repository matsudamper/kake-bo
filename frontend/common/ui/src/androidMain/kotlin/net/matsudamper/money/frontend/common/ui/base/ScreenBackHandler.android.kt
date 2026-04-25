package net.matsudamper.money.frontend.common.ui.base

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
public actual fun ScreenBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(
        enabled = enabled,
        onBack = onBack,
    )
}
