package net.matsudamper.money.frontend.common.viewmodel.addmoneyusage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.add_money_usage.AddMoneyUsageScreenCategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.screen.add_money_usage.AddMoneyUsageScreenUiState
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.AddMoneyUsageScreenSubCategoriesPagingQuery

public class AddMoneyUsageViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlApi: AddMoneyUsageScreenApi,
) {
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
            coroutineScope.launch {
                fetchCategories()
                viewModelStateFlow.update {
                    it.copy(
                        categorySelectDialog = ViewModelState.CategorySelectDialog(
                            categorySet = viewModelStateFlow.value.usageCategorySet,
                            screenType = ViewModelState.CategorySelectDialog.ScreenType.Root,
                        ),
                    )
                }
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
                        categorySelectDialog = if (viewModelState.categorySelectDialog != null) {
                            createCategorySelectDialogUiState(
                                categories = viewModelState.categories,
                                categoryDialogViewModelState = viewModelState.categorySelectDialog,
                                subCategories = viewModelState.subCategories,
                            )
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createCategorySelectDialogUiState(
        categories: List<AddMoneyUsageScreenCategoriesPagingQuery.Node>,
        subCategories: Map<MoneyUsageCategoryId, List<AddMoneyUsageScreenSubCategoriesPagingQuery.Node>>,
        categoryDialogViewModelState: ViewModelState.CategorySelectDialog,
    ): AddMoneyUsageScreenCategorySelectDialogUiState {
        val categorySet = categoryDialogViewModelState.categorySet

        fun changeRootScreen() {
            viewModelStateFlow.update {
                it.copy(
                    categorySelectDialog = categoryDialogViewModelState.copy(
                        screenType = ViewModelState.CategorySelectDialog.ScreenType.Root,
                    ),
                )
            }
        }

        return AddMoneyUsageScreenCategorySelectDialogUiState(
            screenType = when (categoryDialogViewModelState.screenType) {
                ViewModelState.CategorySelectDialog.ScreenType.Root -> {
                    AddMoneyUsageScreenCategorySelectDialogUiState.Screen.Root(
                        category = categorySet.category?.name ?: "未選択",
                        subCategory = categorySet.subCategory?.name ?: "未選択",
                        enableSubCategory = categorySet.category != null,
                        onClickCategory = {
                            viewModelStateFlow.update {
                                it.copy(
                                    categorySelectDialog = categoryDialogViewModelState.copy(
                                        screenType = ViewModelState.CategorySelectDialog.ScreenType.Category,
                                    ),
                                )
                            }
                        },
                        onClickSubCategory = {
                            viewModelStateFlow.update {
                                it.copy(
                                    categorySelectDialog = categoryDialogViewModelState.copy(
                                        screenType = ViewModelState.CategorySelectDialog.ScreenType.SubCategory,
                                    ),
                                )
                            }
                        },
                    )
                }

                ViewModelState.CategorySelectDialog.ScreenType.Category -> {
                    AddMoneyUsageScreenCategorySelectDialogUiState.Screen.Category(
                        categories = categories.map { item ->
                            AddMoneyUsageScreenCategorySelectDialogUiState.Category(
                                name = item.name,
                                isSelected = item.id == categorySet.category?.id,
                                onSelected = {
                                    coroutineScope.launch {
                                        fetchSubCategories(item.id)
                                    }
                                    viewModelStateFlow.update {
                                        it.copy(
                                            categorySelectDialog = categoryDialogViewModelState.copy(
                                                screenType = ViewModelState.CategorySelectDialog.ScreenType.Root,
                                                categorySet = categorySet.copy(
                                                    category = item,
                                                    subCategory = null,
                                                ),
                                            ),
                                        )
                                    }
                                },
                            )
                        }.toImmutableList(),
                        onBackRequest = { changeRootScreen() },
                    )
                }

                ViewModelState.CategorySelectDialog.ScreenType.SubCategory -> {
                    AddMoneyUsageScreenCategorySelectDialogUiState.Screen.SubCategory(
                        subCategories = subCategories[categorySet.category?.id]?.map { item ->
                            AddMoneyUsageScreenCategorySelectDialogUiState.Category(
                                name = item.name,
                                isSelected = item.id == categorySet.subCategory?.id,
                                onSelected = {
                                    viewModelStateFlow.update {
                                        it.copy(
                                            categorySelectDialog = categoryDialogViewModelState.copy(
                                                screenType = ViewModelState.CategorySelectDialog.ScreenType.Root,
                                                categorySet = categorySet.copy(
                                                    subCategory = item,
                                                ),
                                            ),
                                        )
                                    }
                                },
                            )
                        }?.toImmutableList(),
                        onBackRequest = { changeRootScreen() },
                    )
                }
            },
            event = object : AddMoneyUsageScreenCategorySelectDialogUiState.Event {
                override fun dismissRequest() {
                    viewModelStateFlow.update {
                        it.copy(
                            categorySelectDialog = null,
                        )
                    }
                }

                override fun selectCompleted() {
                    viewModelStateFlow.update {
                        it.copy(
                            usageCategorySet = it.categorySelectDialog?.categorySet ?: return,
                            categorySelectDialog = null,
                        )
                    }
                }
            },
        )
    }

    // TODO paging
    // TODO error handling
    private suspend fun fetchCategories() {
        val categories = graphqlApi.getCategories()?.data?.user?.moneyUsageCategories ?: return

        viewModelStateFlow.update {
            it.copy(
                categories = categories.nodes,
            )
        }
    }

    // TODO paging
    // TODO error handling
    private suspend fun fetchSubCategories(id: MoneyUsageCategoryId) {
        val categories = graphqlApi.getSubCategoriesPaging(id = id)?.data?.user?.moneyUsageCategory
            ?.subCategories?.nodes ?: return

        viewModelStateFlow.update {
            it.copy(
                subCategories = it.subCategories
                    .plus(id to categories),
            )
        }
    }

    private data class ViewModelState(
        val usageDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        val usageTitle: String = "",
        val usageDescription: String = "",
        val usageCategorySet: CategorySet = CategorySet(),
        val usageAmount: Int = 0,
        val numberInputDialog: AddMoneyUsageScreenUiState.NumberInputDialog? = null,
        val categorySelectDialog: CategorySelectDialog? = null,
        val categories: List<AddMoneyUsageScreenCategoriesPagingQuery.Node> = listOf(),
        val subCategories: Map<MoneyUsageCategoryId, List<AddMoneyUsageScreenSubCategoriesPagingQuery.Node>> = mapOf(),
        val showCalendarDialog: Boolean = false,
        val textInputDialog: AddMoneyUsageScreenUiState.FullScreenTextInputDialog? = null,
    ) {
        data class CategorySet(
            val category: AddMoneyUsageScreenCategoriesPagingQuery.Node? = null,
            val subCategory: AddMoneyUsageScreenSubCategoriesPagingQuery.Node? = null,
        )

        data class CategorySelectDialog(
            val categorySet: CategorySet,
            val screenType: ScreenType,
        ) {
            enum class ScreenType {
                Root,
                Category,
                SubCategory,
            }
        }
    }
}
