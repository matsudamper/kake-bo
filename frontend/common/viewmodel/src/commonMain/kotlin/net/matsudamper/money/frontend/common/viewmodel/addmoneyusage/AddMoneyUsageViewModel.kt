package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
import kotlinx.datetime.internal.JSJoda.DateTimeFormatter
import kotlinx.datetime.todayIn
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.screen.addmoneyusage.AddMoneyUsageScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter

public class AddMoneyUsageViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlApi: AddMoneyUsageScreenApi,
) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val categorySelectDialogViewModel = object {
        private val event: CategorySelectDialogViewModel.Event = object : CategorySelectDialogViewModel.Event {
            override fun selected(result: CategorySelectDialogViewModel.SelectedResult) {
                viewModelStateFlow.update {
                    it.copy(
                        usageCategorySet = result,
                    )
                }
                viewModel.dismissDialog()
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
            categorySelectDialogViewModel.showDialog(
                categoryId = viewModelStateFlow.value.usageCategorySet?.categoryId,
                categoryName = viewModelStateFlow.value.usageCategorySet?.categoryName,
                subCategoryId = viewModelStateFlow.value.usageCategorySet?.subCategoryId,
                subCategoryName = viewModelStateFlow.value.usageCategorySet?.subCategoryName,
            )
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
    }

    private fun addMoneyUsage() {
        val date = viewModelStateFlow.value.usageDate

        coroutineScope.launch {
            val result = graphqlApi.addMoneyUsage(
                title = viewModelStateFlow.value.usageTitle,
                description = viewModelStateFlow.value.usageDescription,
                datetime = LocalDateTime(
                    date = date,
                    time = viewModelStateFlow.value.usageTime,
                ),
                amount = viewModelStateFlow.value.usageAmount,
                subCategoryId = viewModelStateFlow.value.usageCategorySet?.subCategoryId,
                importedMailId = viewModelStateFlow.value.importedMailId,
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
            coroutineScope.launch {
                eventSender.send {
                    it.navigate(ScreenStructure.AddMoneyUsage())
                }
            }
        }
    }

    private var usageFromMailIdJob: Job = Job()
    public fun updateScreenStructure(current: ScreenStructure.AddMoneyUsage) {
        usageFromMailIdJob.cancel()

        val importedMailId = current.importedMailId
        if (importedMailId == null) {
            viewModelStateFlow.update {
                ViewModelState()
            }
            return
        }

        usageFromMailIdJob = coroutineScope.launch {
            graphqlApi.get(importedMailId)
                .onSuccess { result ->
                    val importedMailIndex = current.importedMailIndex

                    val suggestUsage = result.data?.user?.importedMailAttributes?.mail?.suggestUsages
                        ?.getOrNull(importedMailIndex ?: 0)

                    if (suggestUsage == null) {
                        val subject = result.data?.user?.importedMailAttributes?.mail?.subject.orEmpty()
                        val date = result.data?.user?.importedMailAttributes?.mail?.dateTime
                        viewModelStateFlow.update {
                            it.copy(
                                importedMailId = importedMailId,
                                usageTitle = subject,
                                usageDate = date?.date ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
                            )
                        }
                        return@launch
                    }

                    viewModelStateFlow.update {
                        it.copy(
                            importedMailId = importedMailId,
                            usageAmount = suggestUsage.amount ?: 0,
                            usageDate = suggestUsage.dateTime?.date ?: it.usageDate,
                            usageTime = suggestUsage.dateTime?.time ?: it.usageTime,
                            usageTitle = suggestUsage.title,
                            usageDescription = suggestUsage.description,
                            usageCategorySet = run category@{
                                CategorySelectDialogViewModel.SelectedResult(
                                    categoryId = suggestUsage.subCategory?.category?.id ?: return@category null,
                                    categoryName = suggestUsage.subCategory?.category?.name ?: return@category null,
                                    subCategoryId = suggestUsage.subCategory?.id ?: return@category null,
                                    subCategoryName = suggestUsage.subCategory?.name ?: return@category null,
                                )
                            },
                        )
                    }
                }
                .onFailure {
                    // TODO
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
                            val dayOfWeek = Formatter.dayOfWeekToJapanese(viewModelState.usageDate.dayOfWeek)
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
                            val category = categorySet?.categoryName ?: return@category default
                            val subCategory = categorySet.subCategoryName
                            "$category / $subCategory"
                        },
                        categorySelectDialog = viewModelState.categorySelectDialog,
                    )
                }
            }
        }
    }.asStateFlow()

    public interface Event {
        public fun navigate(structure: ScreenStructure)
    }

    private data class ViewModelState(
        val importedMailId: ImportedMailId? = null,
        val usageDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        val usageTime: LocalTime = LocalTime(0, 0, 0, 0),
        val usageTitle: String = "",
        val usageDescription: String = "",
        val usageAmount: Int = 0,
        val numberInputDialog: AddMoneyUsageScreenUiState.NumberInputDialog? = null,
        val showCalendarDialog: Boolean = false,
        val textInputDialog: AddMoneyUsageScreenUiState.FullScreenTextInputDialog? = null,
        val categorySelectDialog: CategorySelectDialogUiState? = null,
        val usageCategorySet: CategorySelectDialogViewModel.SelectedResult? = null,
    )
}
