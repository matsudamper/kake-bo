package net.matsudamper.money.frontend.common.viewmodel

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

internal class ReservedColorModel(
    private val colorSet: List<Color> = listOf(
        // 薄い赤
        Color(0xFFE57373),
        // 濃い青
        Color(0xFF2979FF),
        // 薄い緑
        Color(0xFF80CBC4),
        // 濃い茶
        Color(0xFF5D4037),
        // 薄い黄
        Color(0xFFFFF59D),
        // 濃い緑
        Color(0xFF00E676),
        // 薄い青
        Color(0xFF90CAF9),
        // 濃いオレンジ
        Color(0xFFFF3D00),
        // 薄い紫
        Color(0xFFE1BEE7),
        // 薄いオレンジ
        Color(0xFFFF8A65),
        // 濃い赤
        Color(0xFFFF1744),
        // 薄いグレー
        Color(0xFFB0BEC5),
        // 濃い黄
        Color(0xFFFFEA00),
        // 薄い茶
        Color(0xFFA1887F),
        // 濃い紫
        Color(0xFFD500F9),
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
}
