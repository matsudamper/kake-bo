package net.matsudamper.money.frontend.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily

@Composable
public actual fun rememberCustomFontFamily(): FontFamily {
    return remember { FontFamily.Default }
}
