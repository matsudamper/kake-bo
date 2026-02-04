package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import kotlin.math.max

public enum class SortSectionType {
    Date,
    Amount,
}

public enum class SortSectionOrder {
    Ascending,
    Descending,
}

@Composable
public fun SortSection(
    currentSortType: SortSectionType,
    sortOrderType: SortSectionOrder,
    onSortTypeChanged: (SortSectionType) -> Unit,
    onSortOrderChanged: (SortSectionOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val maxWidth = this.maxWidth
        Layout(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "並び替え:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Button(
                        onClick = {
                            onSortTypeChanged(SortSectionType.Date)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentSortType == SortSectionType.Date) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("日付順")
                    }
                    Button(
                        onClick = {
                            onSortTypeChanged(SortSectionType.Amount)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentSortType == SortSectionType.Amount) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Text("金額順")
                    }
                }
                var isExpanded by remember { mutableStateOf(false) }
                TextButton(
                    onClick = {
                        isExpanded = !isExpanded
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Sort,
                        contentDescription = null,
                    )
                    DropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = {
                            isExpanded = false
                        },
                        modifier = Modifier,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("昇順")
                            },
                            onClick = {
                                onSortOrderChanged(SortSectionOrder.Ascending)
                                isExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text("降順")
                            },
                            onClick = {
                                onSortOrderChanged(SortSectionOrder.Descending)
                                isExpanded = false
                            },
                        )
                    }
                    Text(
                        when (sortOrderType) {
                            SortSectionOrder.Ascending -> "昇順"
                            SortSectionOrder.Descending -> "降順"
                        },
                    )
                }
            },
        ) { measurables, constraints ->
            val typePlaceable = measurables[0].measure(constraints)
            val orderPlaceable = measurables[1].measure(constraints)

            val height = max(typePlaceable.height, orderPlaceable.height)
            layout(
                width = maxWidth.roundToPx(),
                height = height,
            ) {
                typePlaceable.place(
                    x = 0,
                    y = Alignment.CenterVertically.align(typePlaceable.height, height),
                )
                if (typePlaceable.width + orderPlaceable.width > maxWidth.toPx()) {
                    orderPlaceable.place(
                        x = typePlaceable.width,
                        y = Alignment.CenterVertically.align(orderPlaceable.height, height),
                    )
                } else {
                    orderPlaceable.place(
                        x = maxWidth.roundToPx() - orderPlaceable.width,
                        y = Alignment.CenterVertically.align(orderPlaceable.height, height),
                    )
                }
            }
        }
    }
}
