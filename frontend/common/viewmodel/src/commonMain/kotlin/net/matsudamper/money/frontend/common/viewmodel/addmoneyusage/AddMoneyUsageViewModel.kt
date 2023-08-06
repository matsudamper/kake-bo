package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.screen.addmoneyusage.AddMoneyUsageScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel

public class AddMoneyUsageViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlApi: AddMoneyUsageScreenApi,
) {
    private val categorySelectDialogViewModel = object {
        private val event: CategorySelectDialogViewModel.Event = object : CategorySelectDialogViewModel.Event {
            override fun categorySelected(id: MoneyUsageCategoryId) {
                viewModel.fetchSubCategories(id)
            }
        }
        val viewModel = CategorySelectDialogViewModel(
            coroutineScope = coroutineScope,
            event = event,
        )
    }.viewModel

    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(),
    )

    private val uiEvent = object : AddMoneyUsageScreenUiState.Event {
        override fun onClickAdd() {
            addMoneyUsage()
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
                    usageDate = date,
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
                        default = viewModelState.usageDescription,
                        onComplete = { text ->
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    usageDescription = text,
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

        override fun onClickCategoryChange() {
            categorySelectDialogViewModel.showDialog()
            coroutineScope.launch {
                categorySelectDialogViewModel.fetchCategory()
            }
        }

        override fun onClickAmountChange() {
            val dismissRequest = {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        numberInputDialog = null,
                    )
                }
            }
            val onChangeValue: (Int) -> Unit = { amount ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        usageAmount = amount,
                        numberInputDialog = viewModelState.numberInputDialog?.copy(
                            value = amount,
                        ),
                    )
                }
            }
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    numberInputDialog = AddMoneyUsageScreenUiState.NumberInputDialog(
                        value = viewModelState.usageAmount,
                        dismissRequest = dismissRequest,
                        onChangeValue = onChangeValue,
                    ),
                )
            }
        }

        override fun onClickTitleChange() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    textInputDialog = AddMoneyUsageScreenUiState.FullScreenTextInputDialog(
                        title = "タイトル",
                        default = viewModelState.usageTitle,
                        onComplete = { text ->
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    usageTitle = text,
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
    }

    init {
        coroutineScope.launch {
            categorySelectDialogViewModel.getUiStateFlow().collectLatest { categoryUiState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        categorySelectDialog = categoryUiState,
                    )
                }
            }
        }
        coroutineScope.launch {
            categorySelectDialogViewModel.viewModelStateFlow.collectLatest { categoryViewModelState ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        usageCategorySet = categoryViewModelState.usageCategorySet,
                    )
                }
            }
        }
    }

    private fun addMoneyUsage() {
        val date = viewModelStateFlow.value.usageDate

        coroutineScope.launch {
            val result = graphqlApi.addMoneyUsage(
                title = viewModelStateFlow.value.usageTitle,
                description = viewModelStateFlow.value.usageDescription,
                datetime = LocalDateTime(
                    date = date,
                    time = LocalTime(
                        hour = 0,
                        minute = 0,
                        second = 0,
                        nanosecond = 0,
                    ), // TODO
                ),
                amount = viewModelStateFlow.value.usageAmount,
                subCategoryId = viewModelStateFlow.value.usageCategorySet.subCategory?.id,
            )

            // TODO Toast
            if (result?.data?.userMutation?.addUsage == null) {
                // TODO
            } else {
                // TODO
            }

            viewModelStateFlow.update {
                ViewModelState()
            }
        }
    }

    public val uiStateFlow: StateFlow<AddMoneyUsageScreenUiState> = MutableStateFlow(
        AddMoneyUsageScreenUiState(
            calendarDialog = null,
            date = "",
            title = "",
            description = "",
            amount = "",
            fullScreenTextInputDialog = null,
            categorySelectDialog = null,
            numberInputDialog = null,
            category = "",
            event = uiEvent,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        calendarDialog = AddMoneyUsageScreenUiState.CalendarDialog(
                            selectedDate = viewModelState.usageDate,
                        ).takeIf { viewModelState.showCalendarDialog },
                        date = run {
                            val dayOfWeek = when (viewModelState.usageDate.dayOfWeek) {
                                DayOfWeek.MONDAY -> "月"
                                DayOfWeek.TUESDAY -> "火"
                                DayOfWeek.WEDNESDAY -> "水"
                                DayOfWeek.THURSDAY -> "木"
                                DayOfWeek.FRIDAY -> "金"
                                DayOfWeek.SATURDAY -> "土"
                                DayOfWeek.SUNDAY -> "日"
                            }
                            "${viewModelState.usageDate.year}-${viewModelState.usageDate.monthNumber}-${viewModelState.usageDate.dayOfMonth} ($dayOfWeek)"
                        },
                        title = viewModelState.usageTitle,
                        description = viewModelState.usageDescription,
                        fullScreenTextInputDialog = viewModelState.textInputDialog,
                        numberInputDialog = viewModelState.numberInputDialog,
                        amount = viewModelState.usageAmount.toString(),
                        category = run category@{
                            val default = "未選択"
                            val categorySet = viewModelState.usageCategorySet
                            val category = categorySet.category?.name ?: return@category default
                            val subCategory = categorySet.subCategory?.name ?: return@category default
                            "$category / $subCategory"
                        },
                        categorySelectDialog = viewModelState.categorySelectDialog,
                    )
                }
            }
        }
    }.asStateFlow()

    private data class ViewModelState(
        val usageDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        val usageTitle: String = "",
        val usageDescription: String = "",
        val usageAmount: Int = 0,
        val numberInputDialog: AddMoneyUsageScreenUiState.NumberInputDialog? = null,
        val showCalendarDialog: Boolean = false,
        val textInputDialog: AddMoneyUsageScreenUiState.FullScreenTextInputDialog? = null,
        val categorySelectDialog: CategorySelectDialogUiState? = null,
        val usageCategorySet: CategorySelectDialogViewModel.CategorySet = CategorySelectDialogViewModel.CategorySet(),
    )
}
