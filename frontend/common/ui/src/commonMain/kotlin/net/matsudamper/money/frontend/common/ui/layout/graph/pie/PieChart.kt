package net.matsudamper.money.frontend.common.ui.layout.graph.pie

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList

/**
 * Data class representing the UI state for the pie chart
 */
public data class PieChartUiState(
    val items: ImmutableList<Item>,
    val title: String = "",
) {
    /**
     * Data class representing a single item in the pie chart
     */
    public data class Item(
        val color: Color,
        val title: String,
        val value: Long,
        val event: ItemEvent? = null,
    )

    /**
     * Interface for handling item events
     */
    @Immutable
    public interface ItemEvent {
        public fun onClick()
    }
}

/**
 * A reusable pie chart component that displays data as a pie chart
 *
 * @param uiState The UI state for the pie chart
 * @param modifier The modifier to be applied to the component
 * @param showLegend Whether to show the legend or not
 */
@Composable
public fun PieChart(
    uiState: PieChartUiState,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
) {
    val totalValue = uiState.items.sumOf { it.value }.toFloat()
    
    Column(modifier = modifier) {
        if (uiState.title.isNotEmpty()) {
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2
            val center = Offset(size.width / 2, size.height / 2)
            
            var startAngle = 0f
            
            // Draw pie slices
            uiState.items.forEach { item ->
                if (item.value <= 0) return@forEach
                
                val sweepAngle = (item.value / totalValue) * 360f
                
                // Draw filled slice
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                )
                
                // Draw outline
                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2f),
                )
                
                startAngle += sweepAngle
            }
        }
        
        if (showLegend) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                uiState.items.forEach { item ->
                    if (item.value <= 0) return@forEach
                    
                    val percentage = (item.value.toFloat() / totalValue * 100).toInt()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .let { rowModifier ->
                                if (item.event != null) {
                                    rowModifier.clickable { item.event.onClick() }
                                } else {
                                    rowModifier
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}