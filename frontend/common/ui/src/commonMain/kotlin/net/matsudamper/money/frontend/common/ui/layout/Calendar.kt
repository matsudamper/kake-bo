package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@Composable
public fun Calendar(
    modifier: Modifier = Modifier,
    changeSelectedDate: (LocalDate) -> Unit,
    selectedDate: LocalDate,
) {
    val today by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    val latestSelectedDate by rememberUpdatedState(selectedDate)
    var visibleCalendarDate by remember {
        mutableStateOf(selectedDate)
    }

    val currentMonthDateList: List<LocalDate> by remember {
        derivedStateOf {
            val target = visibleCalendarDate
            val firstDay = LocalDate(target.year, target.monthNumber, 1)

            (0 until 31).map { index ->
                firstDay.plus(index, DateTimeUnit.DAY)
            }.filter { it.month == target.month }
        }
    }

    val calendarItems = remember(currentMonthDateList) {
        val offset = when (currentMonthDateList.first().dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }
        (0 until offset).map { null } + currentMonthDateList
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = {
                visibleCalendarDate = visibleCalendarDate.minus(1, DateTimeUnit.MONTH)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "前の月",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            var showYearMonthPicker by remember { mutableStateOf(false) }
            Text(
                modifier = Modifier
                    .padding(12.dp)
                    .clickable {
                        showYearMonthPicker = !showYearMonthPicker
                    },
                textAlign = TextAlign.Center,
                text = "${currentMonthDateList.first().year}年${currentMonthDateList.first().monthNumber}月",
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (showYearMonthPicker) {
                YearMonthPicker(
                    currentYear = visibleCalendarDate.year,
                    currentMonth = visibleCalendarDate.monthNumber,
                    onDateSelected = { year, month ->
                        // Set to the first day of the selected month to ensure validity
                        visibleCalendarDate = LocalDate(year, month, 1)
                        showYearMonthPicker = false
                    },
                    onDismiss = {
                        showYearMonthPicker = false
                    }
                )
            }

            IconButton(onClick = {
                visibleCalendarDate = visibleCalendarDate.plus(1, DateTimeUnit.MONTH)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "後の月",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier,
        ) {
            items(7) {
                Text(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = when (it) {
                        0 -> "日"
                        1 -> "月"
                        2 -> "火"
                        3 -> "水"
                        4 -> "木"
                        5 -> "金"
                        6 -> "土"
                        else -> throw IllegalStateException()
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items(calendarItems) { date ->
                Box(
                    modifier = Modifier
                        .border(1.dp, color = MaterialTheme.colorScheme.inversePrimary)
                        .background(
                            if (latestSelectedDate == date) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                Color.Transparent
                            },
                        ),
                ) {
                    if (date != null) {
                        Text(
                            modifier = Modifier
                                .clickable {
                                    changeSelectedDate(date)
                                }
                                .fillMaxWidth()
                                .padding(8.dp),
                            textAlign = TextAlign.Center,
                            text = date.dayOfMonth.toString(),
                            color = when (today == date) {
                                true -> MaterialTheme.colorScheme.primary
                                false -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }

@Composable
private fun YearMonthPicker(
    currentYear: Int,
    currentMonth: Int,
    onDateSelected: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonthInternal by remember { mutableStateOf(currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Year and Month", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Year Selector
                Text("Select Year", style = MaterialTheme.typography.titleSmall)
                val yearRange = (currentYear - 10)..(currentYear + 10)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(yearRange.toList()) { year ->
                        OutlinedButton(
                            onClick = { selectedYear = year },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            border = if (selectedYear == year) ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp) else ButtonDefaults.outlinedButtonBorder,
                        ) {
                            Text(year.toString())
                        }
                    }
                }
                Text("Selected Year: $selectedYear", style = MaterialTheme.typography.bodyMedium)


                Spacer(modifier = Modifier.height(16.dp))

                // Month Selector
                Text("Select Month", style = MaterialTheme.typography.titleSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    items(12) { monthIndex ->
                        val monthValue = monthIndex + 1
                        OutlinedButton(
                            onClick = { selectedMonthInternal = monthValue },
                            modifier = Modifier.padding(4.dp),
                            border = if (selectedMonthInternal == monthValue) ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp) else ButtonDefaults.outlinedButtonBorder,
                        ) {
                            Text(monthValue.toString())
                        }
                    }
                }
                Text("Selected Month: $selectedMonthInternal", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onDateSelected(selectedYear, selectedMonthInternal)
                        onDismiss() // Call onDismiss to close the dialog
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}
            }
        }
    }
}
