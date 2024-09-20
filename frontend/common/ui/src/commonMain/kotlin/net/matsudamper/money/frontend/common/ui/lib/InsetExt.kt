package net.matsudamper.money.frontend.common.ui.lib

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
internal fun PaddingValues.asWindowInsets(): WindowInsets {
    return WindowInsets(
        left = this.calculateLeftPadding(LocalLayoutDirection.current),
        top = this.calculateTopPadding(),
        right = this.calculateRightPadding(LocalLayoutDirection.current),
        bottom = this.calculateBottomPadding(),
    )
}
