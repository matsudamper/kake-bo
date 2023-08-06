package net.matsudamper.money.frontend.common.ui.screen.addmoneyusage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialog
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.layout.Calendar
import net.matsudamper.money.frontend.common.ui.layout.NumberInput
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class AddMoneyUsageScreenUiState(
    val calendarDialog: CalendarDialog?,
    val fullScreenTextInputDialog: FullScreenTextInputDialog?,
    val categorySelectDialog: CategorySelectDialogUiState?,
    val date: String,
    val title: String,
    val description: String,
    val category: String,
    val amount: String,
    val event: Event,
    val numberInputDialog: NumberInputDialog?,
) {
    public data class NumberInputDialog(
        val value: Int,
        val onChangeValue: (Int) -> Unit,
        val dismissRequest: () -> Unit,
    )

    public data class FullScreenTextInputDialog(
        val title: String,
        val default: String,
        val onComplete: (String) -> Unit,
        val canceled: () -> Unit,
        val isMultiline: Boolean,
    )

    public data class CalendarDialog(
        val selectedDate: LocalDate,
    )

    public interface Event {
        public fun onClickAdd()
        public fun selectedCalendar(date: LocalDate)
        public fun dismissCalendar()
        public fun onClickDateChange()
        public fun onClickTitleChange()
        public fun onClickDescriptionChange()
        public fun onClickCategoryChange()
        public fun onClickAmountChange()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AddMoneyUsageScreen(
    modifier: Modifier = Modifier,
    uiState: AddMoneyUsageScreenUiState,
) {
    if (uiState.fullScreenTextInputDialog != null) {
        HtmlFullScreenTextInput(
            title = uiState.fullScreenTextInputDialog.title,
            onComplete = { text ->
                uiState.fullScreenTextInputDialog.onComplete(text)
            },
            canceled = { uiState.fullScreenTextInputDialog.canceled() },
            default = uiState.fullScreenTextInputDialog.default,
            isMultiline = uiState.fullScreenTextInputDialog.isMultiline,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            KakeBoTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text("追加")
                },
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    modifier = Modifier
                        .padding(12.dp)
                        .widthIn(max = 700.dp)
                        .fillMaxWidth(),
                    onClick = { uiState.event.onClickAdd() },
                ) {
                    Text("追加")
                }
            }
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp)
                    .widthIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Section(
                    title = {
                        Text("日付")
                    },
                    description = {
                        Text(uiState.date)
                    },
                    clickChange = {
                        uiState.event.onClickDateChange()
                    },
                )
                Divider(Modifier.fillMaxWidth().height(1.dp))
                Section(
                    title = {
                        Text("タイトル")
                    },
                    description = {
                        Text(uiState.title)
                    },
                    clickChange = {
                        uiState.event.onClickTitleChange()
                    },
                )
                Divider(Modifier.fillMaxWidth().height(1.dp))
                Section(
                    title = {
                        Text("金額")
                    },
                    description = {
                        Text(uiState.amount)
                    },
                    clickChange = {
                        uiState.event.onClickAmountChange()
                    },
                )
                Divider(Modifier.fillMaxWidth().height(1.dp))
                Section(
                    title = {
                        Text("カテゴリ")
                    },
                    description = {
                        Text(uiState.category)
                    },
                    clickChange = {
                        uiState.event.onClickCategoryChange()
                    },
                )
                Divider(Modifier.fillMaxWidth().height(1.dp))
                Section(
                    title = {
                        Text("説明")
                    },
                    description = {
                        Text(uiState.description)
                    },
                    clickChange = {
                        uiState.event.onClickDescriptionChange()
                    },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (uiState.calendarDialog != null) {
        CalendarDialog(
            initialCalendar = uiState.calendarDialog.selectedDate,
            dismissRequest = {
                uiState.event.dismissCalendar()
            },
            selectedCalendar = {
                uiState.event.selectedCalendar(it)
            },
        )
    }

    if (uiState.categorySelectDialog != null) {
        CategorySelectDialog(
            uiState = uiState.categorySelectDialog,
        )
    }

    if (uiState.numberInputDialog != null) {
        NumberInputDialog(
            value = uiState.numberInputDialog.value,
            onChangeValue = { uiState.numberInputDialog.onChangeValue(it) },
            dismissRequest = { uiState.numberInputDialog.dismissRequest() },
        )
    }
}

@Composable
private fun NumberInputDialog(
    value: Int,
    onChangeValue: (Int) -> Unit,
    dismissRequest: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                dismissRequest()
            },
        contentAlignment = Alignment.Center,
    ) {
        NumberInput(
            modifier = Modifier.widthIn(max = 500.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    // Number Inputのタッチ無効範囲を触ってもダイアログを閉じないようにする
                },
            value = value,
            onChangeValue = { onChangeValue(it) },
            dismissRequest = { dismissRequest() },
        )
    }
}

@Composable
private fun Section(
    modifier: Modifier = Modifier,
    clickChange: () -> Unit,
    title: @Composable () -> Unit,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    description: @Composable () -> Unit,
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Row(modifier.padding(vertical = 12.dp)) {
        Column {
            ProvideTextStyle(titleStyle) {
                title()
            }
            Spacer(Modifier.height(4.dp))
            ProvideTextStyle(descriptionStyle) {
                description()
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { clickChange() }) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                Text("変更")
            }
        }
    }
}

@Composable
private fun CalendarDialog(
    modifier: Modifier = Modifier,
    dismissRequest: () -> Unit,
    selectedCalendar: (LocalDate) -> Unit,
    initialCalendar: LocalDate,
) {
    Box(
        modifier = modifier.fillMaxSize()
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
                    .padding(24.dp),
            ) {
                var height by remember { mutableStateOf(0) }
                var selectedDate by remember { mutableStateOf(initialCalendar) }
                Calendar(
                    modifier = Modifier
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
