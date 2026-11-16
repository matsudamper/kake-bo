package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
    val locale = currentCalendarLocaleWithRegion()
    val datePickerState = rememberSaveable(saver = datePickerStateSaver(locale)) {
        DatePickerState(
            locale = locale,
            initialSelectedDateMillis = initialCalendar.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
    }

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

/** ロケールを指定する DatePickerState には標準の Saver が公開されていないため、同じ項目を保存する */
@OptIn(ExperimentalMaterial3Api::class)
private fun datePickerStateSaver(locale: CalendarLocale): Saver<DatePickerState, Any> = listSaver(
    save = { state ->
        listOf(
            state.selectedDateMillis,
            state.displayedMonthMillis,
            state.yearRange.first,
            state.yearRange.last,
            state.displayMode == DisplayMode.Input,
        )
    },
    restore = { value ->
        DatePickerState(
            locale = locale,
            initialSelectedDateMillis = value[0] as Long?,
            initialDisplayedMonthMillis = value[1] as Long?,
            yearRange = IntRange(value[2] as Int, value[3] as Int),
            initialDisplayMode = if (value[4] as Boolean) DisplayMode.Input else DisplayMode.Picker,
        )
    },
)
