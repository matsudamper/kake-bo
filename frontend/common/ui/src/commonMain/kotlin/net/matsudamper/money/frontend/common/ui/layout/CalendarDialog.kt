package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarDialog(
    modifier: Modifier = Modifier,
    dismissRequest: () -> Unit,
    selectedCalendar: (LocalDate) -> Unit,
    initialCalendar: LocalDate,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialCalendar.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
    )

    DatePickerDialog(
        modifier = modifier,
        onDismissRequest = dismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC)
                            .date
                        selectedCalendar(date)
                    }
                },
            ) {
                Text(text = "決定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = dismissRequest,
            ) {
                Text(text = "キャンセル")
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
        )
    }
}
