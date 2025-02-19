package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList

public data class GraphTitleChipUiState(
    val title: String,
    val color: Color,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun GraphTitleChips(
    modifier: Modifier = Modifier,
    items: ImmutableList<GraphTitleChipUiState>,
) {
    FlowRow(modifier = modifier) {
        items.forEach { item ->
            AssistChip(
                onClick = {
                    item.onClick()
                },
                label = {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(item.color),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(item.title)
                    }
                },
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}
