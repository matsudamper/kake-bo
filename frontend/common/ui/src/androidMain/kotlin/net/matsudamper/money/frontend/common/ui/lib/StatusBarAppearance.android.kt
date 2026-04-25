package net.matsudamper.money.frontend.common.ui.lib

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
internal actual fun StatusBarAppearance(isLightStatusBar: Boolean) {
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode) {
        DisposableEffect(isLightStatusBar) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLightStatusBar
            onDispose {
                controller.isAppearanceLightStatusBars = !isDarkTheme
            }
        }
    }
}
