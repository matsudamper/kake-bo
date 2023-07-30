package net.matsudamper.money.frontend.common.viewmodel.add_money_usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import net.matsudamper.money.frontend.common.ui.screen.add_money_usage.AddMoneyUsageScreenUiState

public class AddMoneyUsageViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        ),
    )

    public val uiStateFlow: StateFlow<AddMoneyUsageScreenUiState> = MutableStateFlow(
        AddMoneyUsageScreenUiState(
            calendarDialog = null,
            date = "",
            title = "",
            description = "",
            fullScreenTextInputDialog = null,
            event = object : AddMoneyUsageScreenUiState.Event {
                override fun onClickAdd() {

                }

                override fun dismissCalendar() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            showCalendarDialog = false,
                        )
                    }
                }

                override fun selectedCalendar(date: LocalDate) {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            date = date,
                            showCalendarDialog = false,
                        )
                    }
                }

                override fun onClickDateChange() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            showCalendarDialog = true,
                        )
                    }
                }

                override fun onClickDescriptionChange() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            textInputDialog = AddMoneyUsageScreenUiState.FullScreenTextInputDialog(
                                title = "説明",
                                default = viewModelState.description,
                                onComplete = { text ->
                                    viewModelStateFlow.update { viewModelState ->
                                        viewModelState.copy(
                                            description = text,
                                        )
                                    }
                                    dismissTextInputDialog()
                                },
                                canceled = { dismissTextInputDialog() },
                                isMultiline = true,
                            ),
                        )
                    }
                }

                override fun onClickTitleChange() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            textInputDialog = AddMoneyUsageScreenUiState.FullScreenTextInputDialog(
                                title = "タイトル",
                                default = viewModelState.title,
                                onComplete = { text ->
                                    viewModelStateFlow.update { viewModelState ->
                                        viewModelState.copy(
                                            title = text,
                                        )
                                    }
                                    dismissTextInputDialog()
                                },
                                canceled = { dismissTextInputDialog() },
                                isMultiline = false,
                            ),
                        )
                    }
                }

                private fun dismissTextInputDialog() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            textInputDialog = null,
                        )
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        calendarDialog = AddMoneyUsageScreenUiState.CalendarDialog(
                            selectedDate = viewModelState.date,
                        ).takeIf { viewModelState.showCalendarDialog },
                        date = run {
                            val dayOfWeek = when (viewModelState.date.dayOfWeek) {
                                DayOfWeek.MONDAY -> "月"
                                DayOfWeek.TUESDAY -> "火"
                                DayOfWeek.WEDNESDAY -> "水"
                                DayOfWeek.THURSDAY -> "木"
                                DayOfWeek.FRIDAY -> "金"
                                DayOfWeek.SATURDAY -> "土"
                                DayOfWeek.SUNDAY -> "日"
                            }
                            "${viewModelState.date.year}-${viewModelState.date.monthNumber}-${viewModelState.date.dayOfMonth} ($dayOfWeek)"
                        },
                        title = viewModelState.title,
                        description = viewModelState.description,
                        fullScreenTextInputDialog = viewModelState.textInputDialog,
                    )
                }
            }
        }
    }.asStateFlow()

    public data class ViewModelState(
        val date: LocalDate,
        val title: String = "",
        val description: String = "",
        val showCalendarDialog: Boolean = false,
        val textInputDialog: AddMoneyUsageScreenUiState.FullScreenTextInputDialog? = null
    )
}
