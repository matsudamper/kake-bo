package net.matsudamper.money.frontend.common.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design のウィンドウサイズクラス（幅）
 */
public enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded,
    ;

    public companion object {
        public fun fromWidth(width: Dp): WindowWidthSizeClass {
            return when {
                width < 600.dp -> Compact
                width < 840.dp -> Medium
                else -> Expanded
            }
        }
    }
}

public val LocalWindowWidthSizeClass: ProvidableCompositionLocal<WindowWidthSizeClass> = staticCompositionLocalOf<WindowWidthSizeClass> { error("LocalWindowWidthSizeClass") }
