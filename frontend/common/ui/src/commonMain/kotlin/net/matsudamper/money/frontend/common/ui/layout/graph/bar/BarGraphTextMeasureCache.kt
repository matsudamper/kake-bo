package net.matsudamper.money.frontend.common.ui.layout.graph.bar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf

@Stable
internal class BarGraphTextMeasureCache(
    private val textMeasurer: TextMeasurer,
) {
    private var items: ImmutableList<BarGraphUiState.PeriodData> by mutableStateOf(immutableListOf())
    fun updateItems(
        items: ImmutableList<BarGraphUiState.PeriodData>,
    ) {
        this.items = items
    }

    val initialized: Boolean by derivedStateOf { items.isNotEmpty() }

    val minTextMeasureResult by derivedStateOf {
        val minTotalValue = items.minOfOrNull { it.total } ?: 0
        textMeasurer.measure(
            text = AnnotatedString(minTotalValue.toString()),
            constraints = Constraints(),
        )
    }
    val maxTextMeasureResult by derivedStateOf {
        val maxTotalValue = items.maxOfOrNull { it.total } ?: 0
        textMeasurer.measure(
            text = AnnotatedString(maxTotalValue.toString()),
            constraints = Constraints(),
        )
    }

    val xLabels by derivedStateOf {
        items.mapIndexed { index, item ->
            if (index == 0 || item.month == 1) {
                textMeasurer.measure("${item.year}/${item.month}")
            } else {
                textMeasurer.measure("${item.month}")
            }
        }
    }

    val yLabelMaxWidth by derivedStateOf {
        minTextMeasureResult.size.width
            .coerceAtLeast(maxTextMeasureResult.size.width)
    }
}
