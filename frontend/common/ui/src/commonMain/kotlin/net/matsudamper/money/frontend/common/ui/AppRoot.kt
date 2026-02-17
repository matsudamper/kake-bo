package net.matsudamper.money.frontend.common.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
public fun AppRoot(
    fontFamilyResolver: FontFamily.Resolver = LocalFontFamilyResolver.current,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val customColors = if (isDarkTheme) CustomColors.Dark else CustomColors.Light
    val colorScheme = if (isDarkTheme) darkColorScheme(customColors) else lightColorScheme(customColors)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(),
    ) {
        CompositionLocalProvider(
            LocalCustomColors provides customColors,
        ) {
            BoxWithConstraints {
                val maxWidth by rememberUpdatedState(maxWidth)
                val isLargeScreen by remember {
                    derivedStateOf {
                        maxWidth > 800.dp
                    }
                }

                CompositionLocalProvider(
                    LocalFontFamilyResolver provides fontFamilyResolver,
                    LocalTextStyle provides LocalTextStyle.current.merge(
                        TextStyle(
                            fontFamily = rememberCustomFontFamily(),
                        ),
                    ).merge(MaterialTheme.typography.bodyMedium),
                    LocalIsLargeScreen provides isLargeScreen,
                ) {
                    content()
                }
            }
        }
    }
}

private fun darkColorScheme(customColors: CustomColors): ColorScheme {
    return androidx.compose.material3.lightColorScheme().copy(
        primary = Color(0xff8BC34A),
        onPrimary = Color.White,
        background = customColors.backgroundColor,
        onBackground = Color.White,
        surface = customColors.backgroundColor,
        surfaceContainerHighest = customColors.surfaceColor,
        onSurface = Color.White,
        onSecondary = Color.Green,
        onSurfaceVariant = Color.White,
        outlineVariant = Color.LightGray,
        surfaceVariant = customColors.surfaceColor,
        surfaceContainer = customColors.surfaceColor,
        error = Color(0xffFF6075),
        surfaceContainerHigh = customColors.surfaceColor,
        surfaceContainerLow = customColors.surfaceColor,
        surfaceContainerLowest = customColors.surfaceColor,
    )
}

private fun lightColorScheme(customColors: CustomColors): ColorScheme {
    return androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF558B2F),
        onPrimary = Color.White,
        background = customColors.backgroundColor,
        onBackground = Color(0xFF1C1B1F),
        surface = customColors.surfaceColor,
        surfaceContainerHighest = Color(0xFFE0E0E0),
        onSurface = Color(0xFF1C1B1F),
        secondary = Color(0xFF558B2F),
        onSecondary = Color.White,
        onSurfaceVariant = Color(0xFF49454F),
        outlineVariant = Color(0xFFCAC4D0),
        surfaceVariant = Color(0xFFEEEEEE),
        surfaceContainer = Color(0xFFF0F0F0),
        error = Color(0xFFB3261E),
        surfaceContainerHigh = Color(0xFFE8E8E8),
        surfaceContainerLow = Color(0xFFF8F8F8),
        surfaceContainerLowest = Color.White,
    )
}

@Composable
private fun getTypography(): Typography {
    return Typography(
        displayLarge = MaterialTheme.typography.displayLarge
            .applyCustomFontFamily(),
        displayMedium = MaterialTheme.typography.displayMedium
            .applyCustomFontFamily(),
        displaySmall = MaterialTheme.typography.displaySmall
            .applyCustomFontFamily(),
        headlineLarge = MaterialTheme.typography.headlineLarge
            .applyCustomFontFamily(),
        headlineMedium = MaterialTheme.typography.headlineMedium
            .applyCustomFontFamily(),
        headlineSmall = MaterialTheme.typography.headlineSmall
            .applyCustomFontFamily(),
        titleLarge = MaterialTheme.typography.titleLarge
            .applyCustomFontFamily(),
        titleMedium = MaterialTheme.typography.titleMedium
            .applyCustomFontFamily(),
        titleSmall = MaterialTheme.typography.titleSmall
            .applyCustomFontFamily(),
        bodyLarge = MaterialTheme.typography.bodyLarge
            .applyCustomFontFamily(),
        bodyMedium = MaterialTheme.typography.bodyMedium
            .applyCustomFontFamily(),
        bodySmall = MaterialTheme.typography.bodySmall
            .applyCustomFontFamily(),
        labelLarge = MaterialTheme.typography.labelLarge
            .applyCustomFontFamily(),
        labelMedium = MaterialTheme.typography.labelMedium
            .applyCustomFontFamily(),
        labelSmall = MaterialTheme.typography.labelSmall
            .applyCustomFontFamily(),
    )
}

@Composable
private fun TextStyle.applyCustomFontFamily(): TextStyle {
    return copy(
        fontFamily = rememberCustomFontFamily(),
    )
}
