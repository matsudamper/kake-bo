package net.matsudamper.money.frontend.common.ui

import androidx.compose.foundation.layout.BoxWithConstraints
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
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xff8BC34A),
            // Color(0xff444654),
            onPrimary = Color.White,
            background = CustomColors.backgroundColor,
            onBackground = Color.White,
            surface = CustomColors.backgroundColor,
            surfaceContainerHighest = CustomColors.surfaceColor,
            onSurface = Color.White,
            onSecondary = Color.Green,
            // Card->Text, Button->Icon/Text
            onSurfaceVariant = Color.White,
            // Divider
            outlineVariant = Color.LightGray,
            // Card
            surfaceVariant = CustomColors.surfaceColor,
            surfaceContainer = CustomColors.surfaceColor,
            error = Color(0xffFF6075),
        ),
        typography = getTypography(),
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
