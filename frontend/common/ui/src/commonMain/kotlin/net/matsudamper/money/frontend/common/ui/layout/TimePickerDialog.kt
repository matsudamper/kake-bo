package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
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
import kotlinx.datetime.LocalTime

private val ItemHeight = 48.dp
private const val PickerRowCount = 5

@Composable
internal fun TimePickerDialog(
    initialTime: LocalTime,
    dismissRequest: () -> Unit,
    selectedTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            dismissRequest()
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedTime(LocalTime(selectedHour, selectedMinute))
                },
            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            Button(
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
                initialHour = initialTime.hour,
                initialMinute = initialTime.minute,
                onHourChanged = { selectedHour = it },
                onMinuteChanged = { selectedMinute = it },
            )
        },
    )
}

@Composable
private fun TimePicker(
    initialHour: Int,
    initialMinute: Int,
    onHourChanged: (Int) -> Unit,
    onMinuteChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "時",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            DrumPicker(
                items = hours,
                initialIndex = initialHour,
                itemHeight = ItemHeight,
                rows = PickerRowCount,
                onSelectedIndexChanged = { hour ->
                    onHourChanged(hour)
                },
                itemContent = { hour, isSelected ->
                    Text(
                        text = hour.toString().padStart(2, '0'),
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

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterVertically),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "分",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            DrumPicker(
                items = minutes,
                initialIndex = initialMinute,
                itemHeight = ItemHeight,
                rows = PickerRowCount,
                onSelectedIndexChanged = { minute ->
                    onMinuteChanged(minute)
                },
                itemContent = { minute, isSelected ->
                    Text(
                        text = minute.toString().padStart(2, '0'),
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
