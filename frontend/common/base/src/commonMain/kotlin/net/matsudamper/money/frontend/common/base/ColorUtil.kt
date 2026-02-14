package net.matsudamper.money.frontend.common.base

import androidx.compose.ui.graphics.Color

public object ColorUtil {
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
        val colorString = hex.removePrefix("#")
        if (colorString.length != 6) return false
        return colorString.toLongOrNull(16) != null
    }

    public fun normalizeHexColorOrNull(hex: String): String? {
        val normalized = "#" + hex.removePrefix("#").uppercase()
        return normalized.takeIf { isValidHexColor(it) }
    }
}
