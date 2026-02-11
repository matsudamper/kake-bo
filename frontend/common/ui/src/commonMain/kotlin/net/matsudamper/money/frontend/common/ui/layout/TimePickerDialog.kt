package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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

    Box(
        modifier = modifier.zIndex(Float.MAX_VALUE)
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                dismissRequest()
            },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 500.dp),
            ) {
                Text(
                    text = "時間を選択",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                TimePicker(
                    initialHour = initialTime.hour,
                    initialMinute = initialTime.minute,
                    onHourChanged = { selectedHour = it },
                    onMinuteChanged = { selectedMinute = it },
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                ) {
                    TextButton(
                        onClick = { dismissRequest() },
                    ) {
                        Text(text = "キャンセル")
                    }
                    TextButton(
                        onClick = { selectedTime(LocalTime(selectedHour, selectedMinute)) },
                    ) {
                        Text(text = "決定")
                    }
                }
            }
        }
    }
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
