package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.LocalDate

@Composable
internal fun YearMonthPickerDialog(
    initialDate: LocalDate,
    onDismissRequest: () -> Unit,
    onYearMonthChange: (year: Int, month: Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(initialDate.year) }
    var selectedMonth by remember { mutableStateOf(initialDate.monthNumber) }
    Dialog(onDismissRequest = onDismissRequest) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${'$'}{selectedYear}年${'$'}{selectedMonth}月",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val yearRange = (selectedYear - 50)..(selectedYear + 50)
                        yearRange.forEach { year ->
                            Text(
                                text = "${'$'}year年",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedYear = year },
                                textAlign = TextAlign.Center,
                                color = if (year == selectedYear) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        (1..12).forEach { month ->
                            Text(
                                text = "${'$'}month月",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedMonth = month },
                                textAlign = TextAlign.Center,
                                color = if (month == selectedMonth) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("キャンセル")
                    }
                    TextButton(onClick = {
                        onYearMonthChange(selectedYear, selectedMonth)
                    }) {
                        Text("決定")
                    }
                }
            }
        }
    }
}