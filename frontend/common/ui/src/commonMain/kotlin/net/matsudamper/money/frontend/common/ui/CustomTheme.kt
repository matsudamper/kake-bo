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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
public fun CustomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xff8BC34A),
            onPrimary = Color.White, // Color(0xff444654),
            background = CustomColors.backgroundColor,
            surface = CustomColors.backgroundColor,
            onSurface = Color.White,
            onSecondary = Color.Green,
            onSurfaceVariant = Color.White, // Card->Text, Button->Icon/Text
            outlineVariant = Color.LightGray, // Divider
            surfaceVariant = CustomColors.surfaceColor, // Card
            error = Color(0xffFF6075),
        ),
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.applyCustomFontFamily(),
            displayMedium = MaterialTheme.typography.displayMedium.applyCustomFontFamily(),
            displaySmall = MaterialTheme.typography.displaySmall.applyCustomFontFamily(),
            headlineLarge = MaterialTheme.typography.headlineLarge.applyCustomFontFamily(),
            headlineMedium = MaterialTheme.typography.headlineMedium.applyCustomFontFamily(),
            headlineSmall = MaterialTheme.typography.headlineSmall.applyCustomFontFamily(),
            titleLarge = MaterialTheme.typography.titleLarge.applyCustomFontFamily(),
            titleMedium = MaterialTheme.typography.titleMedium.applyCustomFontFamily(),
            titleSmall = MaterialTheme.typography.titleSmall.applyCustomFontFamily(),
            bodyLarge = MaterialTheme.typography.bodyLarge.merge(
                TextStyle(
                    fontSize = 22.sp
                )
            ).applyCustomFontFamily(),
            bodyMedium = MaterialTheme.typography.bodyMedium.merge(
                TextStyle(
                    fontSize = 18.sp
                )
            ).applyCustomFontFamily(),
            bodySmall = MaterialTheme.typography.bodySmall.merge(
                TextStyle(
                    fontSize = 14.sp
                )
            ).applyCustomFontFamily(),
            labelLarge = MaterialTheme.typography.labelLarge.applyCustomFontFamily(),
            labelMedium = MaterialTheme.typography.labelMedium.applyCustomFontFamily(),
            labelSmall = MaterialTheme.typography.labelSmall.applyCustomFontFamily(),
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

@Composable
private fun TextStyle.applyCustomFontFamily(): TextStyle {
    return copy(
        fontFamily = rememberCustomFontFamily(),
    )
}
