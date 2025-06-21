package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.datetime.LocalDate

@Composable
internal fun CalendarDialog(
    modifier: Modifier = Modifier,
    dismissRequest: () -> Unit,
    selectedCalendar: (LocalDate) -> Unit,
    initialCalendar: LocalDate,
) {
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
                ) {
                    // カード内をタップしても閉じないようにする
                },
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 500.dp),
            ) {
                var selectedDate by remember { mutableStateOf(initialCalendar) }
                var showYearMonthPicker by remember { mutableStateOf(false) }
                var visibleCalendarDate by remember { mutableStateOf(initialCalendar) }

                if (showYearMonthPicker) {
                    CalendarAlertDialogScaffold(
                        dismissRequest = {
                            showYearMonthPicker = false
                        },
                        onClickDone = {
                            selectedDate = visibleCalendarDate
                            showYearMonthPicker = false
                        },
                    ) {
                        YearMonthPicker(
                            initialDate = visibleCalendarDate,
                            onDateChanged = { newDate ->
                                visibleCalendarDate = newDate
                                selectedDate = newDate
                            },
                        )
                    }
                } else {
                    CalendarAlertDialogScaffold(
                        dismissRequest = {
                            dismissRequest()
                        },
                        onClickDone = {
                            selectedCalendar(selectedDate)
                        },
                    ) {
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
                }
            }
        }
    }
}

@Composable
private fun CalendarAlertDialogScaffold(
    modifier: Modifier = Modifier,
    dismissRequest: () -> Unit,
    onClickDone: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        content()
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .align(Alignment.End),
        ) {
            TextButton(
                onClick = {
                    dismissRequest()
                },
            ) {
                Text(text = "キャンセル")
            }
            TextButton(
                onClick = {
                    onClickDone()
                },
            ) {
                Text(text = "決定")
            }
        }
    }
}