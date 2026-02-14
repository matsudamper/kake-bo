package net.matsudamper.money.frontend.common.base

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

public data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
) {
    public fun toColor(): Color {
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

    public fun toHexString(): String {
        val color = toColor()
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return "#${toHex2(r)}${toHex2(g)}${toHex2(b)}"
    }

    public companion object {
        public fun fromColor(color: Color): HsvColor {
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

        public fun fromHex(hex: String): HsvColor? {
            val colorString = hex.removePrefix("#")
            if (colorString.length != 6) return null
            val colorLong = colorString.toLongOrNull(16) ?: return null
            val color = Color(
                red = ((colorLong shr 16) and 0xFF).toInt(),
                green = ((colorLong shr 8) and 0xFF).toInt(),
                blue = (colorLong and 0xFF).toInt(),
            )
            return fromColor(color)
        }

        private fun toHex2(value: Int): String {
            return value.toString(16).uppercase().padStart(2, '0')
        }
    }
}
