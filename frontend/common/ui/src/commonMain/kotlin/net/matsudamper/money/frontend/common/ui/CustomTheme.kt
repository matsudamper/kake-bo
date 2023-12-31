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
import androidx.compose.ui.unit.sp

@Composable
public fun CustomTheme(
    isSmartPhone: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
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
        typography = getTypography(isSmartPhone = isSmartPhone),
    ) {
        CompositionLocalProvider(
            LocalFontFamilyResolver provides remember {
                createFontFamilyResolver()
            },
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
private fun getTypography(isSmartPhone: Boolean): Typography {
    val scale = if (isSmartPhone) 1.2f else 1.0f

    return Typography(
        displayLarge = MaterialTheme.typography.displayLarge
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        displayMedium = MaterialTheme.typography.displayMedium
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        displaySmall = MaterialTheme.typography.displaySmall
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        headlineLarge = MaterialTheme.typography.headlineLarge
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        headlineMedium = MaterialTheme.typography.headlineMedium
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        headlineSmall = MaterialTheme.typography.headlineSmall
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        titleLarge = MaterialTheme.typography.titleLarge
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        titleMedium = MaterialTheme.typography.titleMedium
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        titleSmall = MaterialTheme.typography.titleSmall
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        bodyLarge = MaterialTheme.typography.bodyLarge
            .merge(
                TextStyle(
                    fontSize = 22.sp,
                ),
            )
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        bodyMedium = MaterialTheme.typography.bodyMedium
            .merge(
                TextStyle(
                    fontSize = 18.sp,
                ),
            )
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        bodySmall = MaterialTheme.typography.bodySmall
            .merge(
                TextStyle(
                    fontSize = 14.sp,
                ),
            )
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        labelLarge = MaterialTheme.typography.labelLarge
            .merge(
                TextStyle(
                    fontSize = 18.sp,
                ),
            )
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        labelMedium = MaterialTheme.typography.labelMedium
            .merge(
                TextStyle(
                    fontSize = 16.sp,
                ),
            )
            .applyFontScale(scale)
            .applyCustomFontFamily(),
        labelSmall = MaterialTheme.typography.labelSmall
            .merge(
                TextStyle(
                    fontSize = 14.sp,
                ),
            )
            .applyFontScale(scale)
            .applyCustomFontFamily(),
    )
}

private fun TextStyle.applyFontScale(scale: Float): TextStyle {
    return copy(
        fontSize = fontSize * scale,
    )
}

@Composable
private fun TextStyle.applyCustomFontFamily(): TextStyle {
    return copy(
        fontFamily = rememberCustomFontFamily(),
    )
}
