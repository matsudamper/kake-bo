package net.matsudamper.money.frontend.common.viewmodel

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

internal class ReservedColorModel(
    private val colorSet: List<Color> = listOf(
        Color(0xFF4285F4),
        Color(0xFFEA4335),
        Color(0xFFFBBC05),
        Color(0xFF34A853),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4),
        Color(0xFFFF9800),
        Color(0xFF795548),
        Color(0xFF607D8B),
        Color(0xFFE91E63),
    ),
) {
    private val stateFlow = MutableStateFlow(mapOf<String, Color>())

    fun getColor(tag: String): Color {
        val color = stateFlow.value[tag]
        if (color != null) return color

        val result = stateFlow.updateAndGet { state ->
            val newColor = colorSet.asSequence()
                .filterNot { state.values.contains(it) }
                .firstOrNull()
                ?: colorSet.random()
            state.plus(tag to newColor)
        }
        return result[tag]!!
    }

    fun getColor(tag: String, hexColorOverride: String?): Color {
        if (hexColorOverride != null) {
            return parseHexColor(hexColorOverride)
        }
        return getColor(tag)
    }

    companion object {
        fun parseHexColor(hex: String): Color {
            val colorString = hex.removePrefix("#")
            val colorLong = colorString.toLongOrNull(16) ?: return Color.Gray
            return Color(
                red = ((colorLong shr 16) and 0xFF).toInt(),
                green = ((colorLong shr 8) and 0xFF).toInt(),
                blue = (colorLong and 0xFF).toInt(),
            )
        }
    }
}
