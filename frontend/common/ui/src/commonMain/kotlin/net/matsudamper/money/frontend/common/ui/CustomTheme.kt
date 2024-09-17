package net.matsudamper.money.frontend.common.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

@Composable
public fun CustomTheme(
    fontFamilyResolver: FontFamily.Resolver = LocalFontFamilyResolver.current,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme =
        MaterialTheme.colorScheme.copy(
            primary = Color(0xff8BC34A),
            // Color(0xff444654),
            onPrimary = Color.White,
            background = CustomColors.backgroundColor,
            onBackground = Color.White,
            surface = CustomColors.backgroundColor,
            onSurface = Color.White,
            onSecondary = Color.Green,
            // Card->Text, Button->Icon/Text
            onSurfaceVariant = Color.White,
            // Divider
            outlineVariant = Color.LightGray,
            // Card
            surfaceVariant = CustomColors.surfaceColor,
            error = Color(0xffFF6075),
        ),
        typography = getTypography(),
    ) {
        CompositionLocalProvider(
            LocalFontFamilyResolver provides fontFamilyResolver,
            LocalTextStyle provides LocalTextStyle.current.merge(
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
