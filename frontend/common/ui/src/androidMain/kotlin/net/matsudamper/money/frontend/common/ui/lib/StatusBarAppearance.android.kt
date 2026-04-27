package net.matsudamper.money.frontend.common.ui.lib

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
internal actual fun StatusBarAppearance(isLightStatusBar: Boolean) {
    val view = LocalView.current
    val activity = LocalActivity.current
    val isDarkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode && activity != null) {
        DisposableEffect(isLightStatusBar) {
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLightStatusBar
            onDispose {
                controller.isAppearanceLightStatusBars = !isDarkTheme
            }
        }
    }
}
