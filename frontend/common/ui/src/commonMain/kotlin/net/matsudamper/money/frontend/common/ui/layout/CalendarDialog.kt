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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
        modifier =
        modifier.zIndex(Float.MAX_VALUE)
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
        val density = LocalDensity.current
        Card(
            modifier =
            Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    // カード内をタップしても閉じないようにする
                },
        ) {
            Column(
                modifier =
                Modifier
                    .padding(24.dp),
            ) {
                var height by remember { mutableStateOf(0) }
                var selectedDate by remember { mutableStateOf(initialCalendar) }
                Calendar(
                    modifier =
                    Modifier
                        .widthIn(max = 500.dp)
                        .heightIn(min = with(density) { height.toDp() })
                        .onSizeChanged {
                            height = it.height
                        },
                    selectedDate = selectedDate,
                    changeSelectedDate = {
                        selectedDate = it
                    },
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier =
                    Modifier
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
                            selectedCalendar(selectedDate)
                        },
                    ) {
                        Text(text = "決定")
                    }
                }
            }
        }
    }
}
