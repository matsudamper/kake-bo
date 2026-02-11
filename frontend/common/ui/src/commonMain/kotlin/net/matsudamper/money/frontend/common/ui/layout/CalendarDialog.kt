package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate

@Composable
internal fun CalendarDialog(
    modifier: Modifier = Modifier,
    dismissRequest: () -> Unit,
    selectedCalendar: (LocalDate) -> Unit,
    initialCalendar: LocalDate,
) {
    var selectedDate by remember { mutableStateOf(initialCalendar) }
    var showYearMonthPicker by remember { mutableStateOf(false) }
    var selectingDate by remember { mutableStateOf(initialCalendar) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = dismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    if (showYearMonthPicker) {
                        selectedDate = selectingDate
                        showYearMonthPicker = false
                    } else {
                        selectedCalendar(selectedDate)
                    }
                },
            ) {
                Text(text = "決定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (showYearMonthPicker) {
                        showYearMonthPicker = false
                    } else {
                        dismissRequest()
                    }
                },
            ) {
                Text(text = "キャンセル")
            }
        },
        text = {
            if (showYearMonthPicker) {
                YearMonthPicker(
                    initialDate = selectedDate,
                    onDateChanged = { newDate ->
                        selectingDate = newDate
                    },
                )
            } else {
                Calendar(
                    modifier = Modifier,
                    selectedDate = selectedDate,
                    changeSelectedDate = {
                        selectedDate = it
                    },
                    onYearMonthClick = {
                        showYearMonthPicker = true
                    },
                )
            }
        },
    )
}
