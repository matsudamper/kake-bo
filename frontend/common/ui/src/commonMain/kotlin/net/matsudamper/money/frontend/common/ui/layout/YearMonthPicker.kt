package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

private val ItemHeight = 48.dp
private const val PickerRowCount = 5

@Composable
internal fun YearMonthPicker(
    initialDate: LocalDate,
    modifier: Modifier = Modifier,
    onDateChanged: (LocalDate) -> Unit,
) {
    val years = remember { (initialDate.year - 10..initialDate.year + 10).toList() }
    val months = (1..12).toList()

    var selectedDate by remember { mutableStateOf(initialDate) }

    val initialYearIndex = years.indexOf(initialDate.year).takeIf { it >= 0 } ?: 0
    val initialMonthIndex = initialDate.monthNumber - 1

    Column(
        modifier = modifier,
    ) {
        Text(
            text = "年月を選択",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(16.dp),
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "年",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                DrumPicker(
                    items = years,
                    initialIndex = initialYearIndex,
                    itemHeight = ItemHeight,
                    rows = PickerRowCount,
                    onSelectedIndexChanged = { index ->
                        val newYear = years[index]
                        val newDate = LocalDate(newYear, selectedDate.monthNumber, selectedDate.dayOfMonth)
                        onDateChanged(newDate)
                    },
                    itemContent = { year, isSelected ->
                        Text(
                            text = "${year}年",
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            style = if (isSelected) {
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "月",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                DrumPicker(
                    items = months,
                    initialIndex = initialMonthIndex,
                    itemHeight = ItemHeight,
                    rows = PickerRowCount,
                    onSelectedIndexChanged = { index ->
                        val newMonth = months[index]
                        val newDate = LocalDate(selectedDate.year, newMonth, selectedDate.dayOfMonth)
                        onDateChanged(newDate)
                    },
                    itemContent = { month, isSelected ->
                        Text(
                            text = "${month}月",
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            style = if (isSelected) {
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.bodyLarge
                            },
                        )
                    },
                )
            }
        }
    }
}
