package net.matsudamper.money.frontend.common.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.createFontFamilyResolver
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily

@Composable
public fun CustomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xff8BC34A),
            onPrimary = Color.White,//Color(0xff444654),
            background = CustomColors.backgroundColor,
            surface = CustomColors.backgroundColor,
            onSurface = Color.White,
            onSecondary = Color.Green,
            onSurfaceVariant = Color.White, // Card->Text, Button->Icon/Text
            outlineVariant = Color.DarkGray, // Divider
            surfaceVariant = CustomColors.surfaceColor, // Card
        ),
    ) {
        CompositionLocalProvider(
            LocalFontFamilyResolver provides remember {
                createFontFamilyResolver()
            },
            LocalTextStyle provides LocalTextStyle.current.copy(
                fontFamily = rememberCustomFontFamily(),
            ),
        ) {
            content()
        }
    }
}
