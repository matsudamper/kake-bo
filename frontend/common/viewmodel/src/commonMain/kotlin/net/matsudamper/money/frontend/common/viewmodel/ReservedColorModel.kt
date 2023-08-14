package net.matsudamper.money.frontend.common.viewmodel

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

internal class ReservedColorModel(
    private val colorSet: List<Color> = listOf(
        Color.Blue,
        Color.Magenta,
        Color.Yellow,
        Color.Green,
        Color.Red,
        Color.Cyan,
        Color.LightGray,
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
