package net.matsudamper.money.frontend.common.viewmodel.add_money_usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.add_money_usage.AddMoneyUsageScreenCategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.screen.add_money_usage.AddMoneyUsageScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.settings.SettingScreenCategoryApi
import net.matsudamper.money.frontend.graphql.CategoriesSettingScreenQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenSubCategoriesPagingQuery

public class AddMoneyUsageViewModel(
    private val coroutineScope: CoroutineScope,
    private val settingScreenCategoryApi: SettingScreenCategoryApi, // TODO この画面専用のAPIを作る
) {
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            usageDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        ),
    )

    private val uiEvent = object : AddMoneyUsageScreenUiState.Event {
        override fun onClickAdd() {
            // TODO Implement Graphql
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

    public val uiStateFlow: StateFlow<AddMoneyUsageScreenUiState> = MutableStateFlow(
        AddMoneyUsageScreenUiState(
            calendarDialog = null,
            date = "",
            title = "",
            description = "",
            fullScreenTextInputDialog = null,
            categorySelectDialog = null,
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
                        category = run category@{
                            val default = "未選択"
                            val categorySet = viewModelState.usageCategorySet ?: return@category default
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
        categories: List<CategoriesSettingScreenQuery.Node>,
        subCategories: Map<MoneyUsageCategoryId, List<CategorySettingScreenSubCategoriesPagingQuery.Node>>,
        categoryDialogViewModelState: ViewModelState.CategorySelectDialog,
    ): AddMoneyUsageScreenCategorySelectDialogUiState {
        val categorySet = categoryDialogViewModelState.categorySet

        fun changeRootScreen() {
            viewModelStateFlow.update {
                it.copy(
                    categorySelectDialog = categoryDialogViewModelState.copy(
                        screenType = ViewModelState.CategorySelectDialog.ScreenType.Root,
                    )
                )
            }
        }

        return AddMoneyUsageScreenCategorySelectDialogUiState(
            screenType = when (categoryDialogViewModelState.screenType) {
                ViewModelState.CategorySelectDialog.ScreenType.Root -> {
                    AddMoneyUsageScreenCategorySelectDialogUiState.Screen.Root(
                        category = categorySet.category?.name ?: "未選択",
                        subCategory = categorySet.subCategory?.name ?: "未選択",
                        onClickCategory = {
                            viewModelStateFlow.update {
                                it.copy(
                                    categorySelectDialog = categoryDialogViewModelState.copy(
                                        screenType = ViewModelState.CategorySelectDialog.ScreenType.Category,
                                    )
                                )
                            }
                        },
                        onClickSubCategory = {
                            if (categoryDialogViewModelState.categorySet.category == null) {
                                // TODO カテゴリを先に選択するように促す
                            }
                            viewModelStateFlow.update {
                                it.copy(
                                    categorySelectDialog = categoryDialogViewModelState.copy(
                                        screenType = ViewModelState.CategorySelectDialog.ScreenType.SubCategory,
                                    )
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
                                            )
                                        )
                                    }
                                }
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
                                            )
                                        )
                                    }
                                }
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
        val categories = settingScreenCategoryApi.getCategories()?.data?.user?.moneyUsageCategories ?: return

        viewModelStateFlow.update {
            it.copy(
                categories = categories.nodes
            )
        }
    }

    // TODO paging
    // TODO error handling
    private suspend fun fetchSubCategories(id: MoneyUsageCategoryId) {
        val categories = settingScreenCategoryApi.getSubCategoriesPaging(id = id)?.data?.user?.moneyUsageCategory
            ?.subCategories?.nodes ?: return

        viewModelStateFlow.update {
            it.copy(
                subCategories = it.subCategories
                    .plus(id to categories)
            )
        }
    }

    private data class ViewModelState(
        val usageDate: LocalDate,
        val usageTitle: String = "",
        val usageDescription: String = "",
        val usageCategorySet: CategorySet = CategorySet(),
        val categorySelectDialog: CategorySelectDialog? = null,
        val categories: List<CategoriesSettingScreenQuery.Node> = listOf(),
        val subCategories: Map<MoneyUsageCategoryId, List<CategorySettingScreenSubCategoriesPagingQuery.Node>> = mapOf(),
        val showCalendarDialog: Boolean = false,
        val textInputDialog: AddMoneyUsageScreenUiState.FullScreenTextInputDialog? = null,
    ) {
        data class CategorySet(
            val category: CategoriesSettingScreenQuery.Node? = null,
            val subCategory: CategorySettingScreenSubCategoriesPagingQuery.Node? = null,
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
