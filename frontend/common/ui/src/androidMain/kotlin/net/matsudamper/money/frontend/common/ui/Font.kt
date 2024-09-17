package net.matsudamper.money.frontend.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

@Composable
public actual fun rememberCustomFontFamily(): FontFamily {
    return remember { FontFamily.Default }
}

@Composable
public actual fun rememberFontFamilyResolver(): FontFamily.Resolver {
    val context = LocalContext.current
    return remember(context) { androidx.compose.ui.text.font.createFontFamilyResolver(context) }
}
