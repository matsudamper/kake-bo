package net.matsudamper.money.frontend.common.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver

@Composable
public fun CustomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            MaterialTheme.colorScheme.copy(
                primary = Color(0xff8BC34A),
                onPrimary = Color.White, // Color(0xff444654),
                background = CustomColors.backgroundColor,
                onBackground = Color.White,
                surface = CustomColors.backgroundColor,
                onSurface = Color.White,
                onSecondary = Color.Green,
                onSurfaceVariant = Color.White, // Card->Text, Button->Icon/Text
                outlineVariant = Color.LightGray, // Divider
                surfaceVariant = CustomColors.surfaceColor, // Card
                error = Color(0xffFF6075),
            ),
        typography = getTypography(),
    ) {
        CompositionLocalProvider(
            LocalFontFamilyResolver provides
                remember {
                    createFontFamilyResolver()
                },
            LocalTextStyle provides
                LocalTextStyle.current.merge(
                    TextStyle(
                        fontFamily = rememberCustomFontFamily(),
                    ),
                ).merge(MaterialTheme.typography.bodyMedium),
        ) {
            content()
        }
    }
}

@Composable
private fun getTypography(): Typography {
    return Typography(
        displayLarge =
            MaterialTheme.typography.displayLarge
                .applyCustomFontFamily(),
        displayMedium =
            MaterialTheme.typography.displayMedium
                .applyCustomFontFamily(),
        displaySmall =
            MaterialTheme.typography.displaySmall
                .applyCustomFontFamily(),
        headlineLarge =
            MaterialTheme.typography.headlineLarge
                .applyCustomFontFamily(),
        headlineMedium =
            MaterialTheme.typography.headlineMedium
                .applyCustomFontFamily(),
        headlineSmall =
            MaterialTheme.typography.headlineSmall
                .applyCustomFontFamily(),
        titleLarge =
            MaterialTheme.typography.titleLarge
                .applyCustomFontFamily(),
        titleMedium =
            MaterialTheme.typography.titleMedium
                .applyCustomFontFamily(),
        titleSmall =
            MaterialTheme.typography.titleSmall
                .applyCustomFontFamily(),
        bodyLarge =
            MaterialTheme.typography.bodyLarge
                .applyCustomFontFamily(),
        bodyMedium =
            MaterialTheme.typography.bodyMedium
                .applyCustomFontFamily(),
        bodySmall =
            MaterialTheme.typography.bodySmall
                .applyCustomFontFamily(),
        labelLarge =
            MaterialTheme.typography.labelLarge
                .applyCustomFontFamily(),
        labelMedium =
            MaterialTheme.typography.labelMedium
                .applyCustomFontFamily(),
        labelSmall =
            MaterialTheme.typography.labelSmall
                .applyCustomFontFamily(),
    )
}

@Composable
private fun TextStyle.applyCustomFontFamily(): TextStyle {
    return copy(
        fontFamily = rememberCustomFontFamily(),
    )
}
