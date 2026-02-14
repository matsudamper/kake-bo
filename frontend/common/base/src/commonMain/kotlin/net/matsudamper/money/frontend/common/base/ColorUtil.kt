package net.matsudamper.money.frontend.common.base

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

public object ColorUtil {
    public fun parseHexColor(hex: String): Color? {
        val normalizedHex = normalizeHexColorOrNull(hex) ?: return null
        val colorString = normalizedHex.removePrefix("#")
        val colorLong = colorString.toLongOrNull(16) ?: return null
        return Color(
            red = ((colorLong shr 16) and 0xFF).toInt(),
            green = ((colorLong shr 8) and 0xFF).toInt(),
            blue = (colorLong and 0xFF).toInt(),
        )
    }

    public fun isValidHexColor(hex: String): Boolean {
        val colorString = hex.removePrefix("#")
        if (colorString.length != 6) return false
        return colorString.toLongOrNull(16) != null
    }

    public fun normalizeHexColorOrNull(hex: String): String? {
        val normalized = "#" + hex.removePrefix("#").uppercase()
        return normalized.takeIf { isValidHexColor(it) }
    }

    public fun toHexColor(color: Color): String {
        val red = (color.red * 255f).roundToInt().coerceIn(0, 255)
        val green = (color.green * 255f).roundToInt().coerceIn(0, 255)
        val blue = (color.blue * 255f).roundToInt().coerceIn(0, 255)

        return "${toHex2(red)}${toHex2(green)}${toHex2(blue)}"
    }

    private fun toHex2(value: Int): String {
        return value.toString(16).uppercase().padStart(2, '0')
    }
}
