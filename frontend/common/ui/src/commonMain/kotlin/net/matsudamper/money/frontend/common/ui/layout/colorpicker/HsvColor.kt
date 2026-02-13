package net.matsudamper.money.frontend.common.ui.layout.colorpicker

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

internal fun Int.toHex2(): String {
    return toString(16).uppercase().padStart(2, '0')
}

internal fun HsvColor.toColor(): Color {
    val c = value * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = value - c

    val (r1, g1, b1) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
    )
}

internal fun HsvColor.toHexString(): String {
    val color = toColor()
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return "#${r.toHex2()}${g.toHex2()}${b.toHex2()}"
}

public fun parseHexColor(hex: String): Color {
    val colorString = hex.removePrefix("#")
    if (colorString.length != 6) return Color.Gray
    val colorLong = colorString.toLongOrNull(16) ?: return Color.Gray
    return Color(
        red = ((colorLong shr 16) and 0xFF).toInt(),
        green = ((colorLong shr 8) and 0xFF).toInt(),
        blue = (colorLong and 0xFF).toInt(),
    )
}

public fun isValidHexColor(hex: String): Boolean {
    return hsvFromHex(hex) != null
}

internal fun hsvFromColor(color: Color): HsvColor {
    val r = color.red
    val g = color.green
    val b = color.blue

    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC

    val hue = when {
        delta == 0f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val saturation = if (maxC == 0f) 0f else delta / maxC
    val value = maxC

    return HsvColor(
        hue = hue,
        saturation = saturation,
        value = value,
    )
}

internal fun hsvFromHex(hex: String): HsvColor? {
    val colorString = hex.removePrefix("#")
    if (colorString.length != 6) return null
    val colorLong = colorString.toLongOrNull(16) ?: return null
    val color = Color(
        red = ((colorLong shr 16) and 0xFF).toInt(),
        green = ((colorLong shr 8) and 0xFF).toInt(),
        blue = (colorLong and 0xFF).toInt(),
    )
    return hsvFromColor(color)
}
