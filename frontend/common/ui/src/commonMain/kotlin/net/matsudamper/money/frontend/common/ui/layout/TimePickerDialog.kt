package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(
    initialTime: LocalTime,
    dismissRequest: () -> Unit,
    selectedTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )

    AlertDialog(
        modifier = modifier,
        onDismissRequest = dismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    selectedTime(LocalTime(timePickerState.hour, timePickerState.minute))
                },
            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = dismissRequest,
            ) {
                Text(text = "キャンセル")
            }
        },
        title = {
            Text(text = "時間を選択")
        },
        text = {
            TimePicker(
                state = timePickerState,
            )
        },
    )
}
