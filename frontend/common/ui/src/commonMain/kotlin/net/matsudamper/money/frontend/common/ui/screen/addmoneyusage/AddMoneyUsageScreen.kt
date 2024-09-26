package net.matsudamper.money.frontend.common.ui.screen.addmoneyusage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialog
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.layout.CalendarDialog
import net.matsudamper.money.frontend.common.ui.layout.NumberInput
import net.matsudamper.money.frontend.common.ui.layout.NumberInputValue
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput
import net.matsudamper.money.frontend.common.ui.lib.asWindowInsets

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
        val value: NumberInputValue,
        val onChangeValue: (NumberInputValue) -> Unit,
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

@Composable
public fun AddMoneyUsageScreen(
    modifier: Modifier = Modifier,
    uiState: AddMoneyUsageScreenUiState,
    windowInsets: PaddingValues,
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
                    Box(
                        modifier = Modifier,
                    ) {
                        Text("追加")
                    }
                },
                windowInsets = windowInsets,
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    modifier =
                    Modifier
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
        contentWindowInsets = windowInsets.asWindowInsets(),
    ) { paddingValues ->
        Box(
            modifier =
            Modifier
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
                HorizontalDivider(Modifier.fillMaxWidth().height(1.dp))
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
                HorizontalDivider(Modifier.fillMaxWidth().height(1.dp))
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
                HorizontalDivider(Modifier.fillMaxWidth().height(1.dp))
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
                HorizontalDivider(Modifier.fillMaxWidth().height(1.dp))
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
    value: NumberInputValue,
    onChangeValue: (NumberInputValue) -> Unit,
    dismissRequest: () -> Unit,
) {
    Box(
        modifier =
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                dismissRequest()
            },
        contentAlignment = Alignment.Center,
    ) {
        NumberInput(
            modifier =
            Modifier.widthIn(max = 500.dp)
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
    descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Row(modifier.padding(vertical = 12.dp)) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            ProvideTextStyle(titleStyle) {
                title()
            }
            Spacer(Modifier.height(4.dp))
            ProvideTextStyle(descriptionStyle) {
                description()
            }
        }
        TextButton(onClick = { clickChange() }) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                Text("変更")
            }
        }
    }
}
