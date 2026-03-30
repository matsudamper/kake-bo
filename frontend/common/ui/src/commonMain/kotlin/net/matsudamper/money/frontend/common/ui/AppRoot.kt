package net.matsudamper.money.frontend.common.ui

import androidx.annotation.FloatRange
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Card
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
    val background = Color(0xff343541)
    return androidx.compose.material3.darkColorScheme().copy(
        primary = Color(0xff8BC34A),
        onPrimary = Color.White,

        surface = background, // Toolbar等
        onSurface = Color.White,

        background = background,
        onBackground = Color.White,

        surfaceContainerHighest = background.brighten(0.1f), // Card
        surfaceContainer = background,

        primaryContainer = Color(0xff8BC34A),
        onPrimaryContainer = Color.White,

        onSecondary = Color.Green,
        onSurfaceVariant = Color.White,
        outlineVariant = Color.LightGray,
        error = Color(0xffFF6075),
    )
}

private fun lightColorScheme(customColors: CustomColors): ColorScheme {
    return androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF558B2F),
        onPrimary = Color.White,
        background = Color(0xFFF5F5F5),
        onBackground = Color(0xFF1C1B1F),
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

/**
 * HSVモデルの明度 (Value) を指定した量だけ増やし、新しい [Color] を返します。
 *
 * @param amount 明度に加算する割合。0.0（変化なし）から1.0（最大）の範囲で指定します。
 * @return 明度が調整された新しい [Color] オブジェクト。
 */
private fun Color.brighten(
    @FloatRange(from = 0.0, to = 1.0) amount: Float,
): Color {
    val (h, s, v) = toHsv()
    return fromHsv(h, s, (v + amount).coerceIn(0f, 1f), this.alpha)
}

/**
 * ColorからHSV形式の値を取得する内部関数
 */
private fun Color.toHsv(): Triple<Float, Float, Float> {
    val r = this.red
    val g = this.green
    val b = this.blue

    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val diff = max - min

    val h = when {
        max == min -> 0f
        max == r -> (60 * ((g - b) / diff) + 360) % 360
        max == g -> (60 * ((b - r) / diff) + 120) % 360
        else -> (60 * ((r - g) / diff) + 240) % 360
    }

    val s = if (max == 0f) 0f else diff / max
    val v = max

    return Triple(h, s, v)
}

/**
 * HSV値から新しい Color を作成する内部関数
 */
private fun fromHsv(h: Float, s: Float, v: Float, a: Float): Color {
    val c = v * s
    val x = c * (1 - abs((h / 60f) % 2 - 1))
    val m = v - c

    val (r, g, b) = when (h.toInt() / 60) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(r + m, g + m, b + m, a)
}