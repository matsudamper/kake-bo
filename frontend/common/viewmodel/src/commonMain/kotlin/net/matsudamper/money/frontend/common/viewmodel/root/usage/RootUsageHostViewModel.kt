package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MoneyUsageSelectDialogCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.MoneyUsageSelectDialogSubCategoriesPagingQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput
import net.matsudamper.money.frontend.graphql.type.MoneyUsageSubCategoryQuery

public class RootUsageHostViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    navController: ScreenNavController,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val mutableViewModelStateFlow = MutableStateFlow(ViewModelState())
    public val viewModelStateFlow: StateFlow<ViewModelState> = mutableViewModelStateFlow.asStateFlow()

    private val rootNavigationEventSender = EventSender<RootNavigationEvent>()
    public val rootNavigationEventHandler: EventHandler<RootNavigationEvent> = rootNavigationEventSender.asHandler()
    private val event = object : RootUsageHostScreenUiState.Event {
        override suspend fun onViewInitialized() {
            fetchCategories()
        }

        override fun onClickAdd() {
            viewModelScope.launch {
                rootNavigationEventSender.send {
                    it.navigate(ScreenStructure.AddMoneyUsage())
                }
            }
        }

        override fun onClickCalendar() {
            viewModelScope.launch {
                rootNavigationEventSender.send {
                    it.navigate(ScreenStructure.Root.Usage.Calendar())
                }
            }
        }

        override fun onClickList() {
            viewModelScope.launch {
                rootNavigationEventSender.send {
                    it.navigate(ScreenStructure.Root.Usage.List)
                }
            }
        }

        override fun onClickSearchBox() {
            mutableViewModelStateFlow.update {
                it.copy(
                    textInputUiState = RootUsageHostScreenUiState.TextInputUiState(
                        title = "検索",
                        default = mutableViewModelStateFlow.value.searchText,
                        inputType = TextFieldType.Text,
                        textComplete = { text ->
                            updateSearchText(text)
                            closeTextInput()
                        },
                        canceled = {
                            closeTextInput()
                        },
                        isMultiline = false,
                        name = "",
                    ),
                )
            }
        }

        override fun onClickSearchBoxClear() {
            updateSearchText("")
        }

        private fun closeTextInput() {
            mutableViewModelStateFlow.update {
                it.copy(
                    textInputUiState = null,
                )
            }
        }

        private fun updateSearchText(text: String) {
            mutableViewModelStateFlow.update {
                it.copy(
                    searchText = text,
                )
            }
        }
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            val response = graphqlClient.apolloClient.query(
                MoneyUsageSelectDialogCategoriesPagingQuery(
                    MoneyUsageCategoriesInput(
                        size = 100,
                        cursor = Optional.present(null),
                    ),
                ),
            )
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()

            val categories = response.data?.user?.moneyUsageCategories?.nodes.orEmpty()
            mutableViewModelStateFlow.update {
                it.copy(
                    categories = categories.map { node ->
                        CategoryInfo(
                            id = node.id,
                            name = node.name,
                        )
                    },
                )
            }
        }
    }

    private fun fetchSubCategories(categoryId: MoneyUsageCategoryId) {
        viewModelScope.launch {
            val response = graphqlClient.apolloClient.query(
                MoneyUsageSelectDialogSubCategoriesPagingQuery(
                    categoryId = categoryId,
                    query = MoneyUsageSubCategoryQuery(
                        size = 100,
                        cursor = Optional.present(null),
                    ),
                ),
            )
                .fetchPolicy(FetchPolicy.CacheFirst)
                .execute()

            val subCategories = response.data?.user?.moneyUsageCategory?.subCategories?.nodes.orEmpty()
            mutableViewModelStateFlow.update {
                it.copy(
                    subCategories = subCategories.map { node ->
                        SubCategoryInfo(
                            id = node.id,
                            name = node.name,
                        )
                    },
                )
            }
        }
    }

    private fun selectCategory(categoryId: MoneyUsageCategoryId?) {
        val current = mutableViewModelStateFlow.value.selectedCategoryId
        if (current == categoryId) return
        mutableViewModelStateFlow.update {
            it.copy(
                selectedCategoryId = categoryId,
                selectedSubCategoryId = null,
                subCategories = listOf(),
            )
        }
        if (categoryId != null) {
            fetchSubCategories(categoryId)
        }
    }

    private fun selectSubCategory(subCategoryId: MoneyUsageSubCategoryId?) {
        mutableViewModelStateFlow.update {
            it.copy(
                selectedSubCategoryId = subCategoryId,
            )
        }
    }

    private fun createCategoryFilterState(viewModelState: ViewModelState): RootUsageHostScreenUiState.CategoryFilterState {
        val allLabel = "全て"

        val categoryItems = buildList {
            add(
                RootUsageHostScreenUiState.DropdownItem(
                    name = allLabel,
                    event = object : RootUsageHostScreenUiState.DropdownItemEvent {
                        override fun onClick() {
                            selectCategory(null)
                        }
                    },
                ),
            )
            viewModelState.categories.forEach { category ->
                add(
                    RootUsageHostScreenUiState.DropdownItem(
                        name = category.name,
                        event = object : RootUsageHostScreenUiState.DropdownItemEvent {
                            override fun onClick() {
                                selectCategory(category.id)
                            }
                        },
                    ),
                )
            }
        }.toImmutableList()

        val selectedCategoryName = if (viewModelState.selectedCategoryId != null) {
            viewModelState.categories
                .firstOrNull { it.id == viewModelState.selectedCategoryId }
                ?.name ?: allLabel
        } else {
            allLabel
        }

        val subCategoryItems = buildList {
            add(
                RootUsageHostScreenUiState.DropdownItem(
                    name = allLabel,
                    event = object : RootUsageHostScreenUiState.DropdownItemEvent {
                        override fun onClick() {
                            selectSubCategory(null)
                        }
                    },
                ),
            )
            viewModelState.subCategories.forEach { subCategory ->
                add(
                    RootUsageHostScreenUiState.DropdownItem(
                        name = subCategory.name,
                        event = object : RootUsageHostScreenUiState.DropdownItemEvent {
                            override fun onClick() {
                                selectSubCategory(subCategory.id)
                            }
                        },
                    ),
                )
            }
        }.toImmutableList()

        val selectedSubCategoryName = if (viewModelState.selectedSubCategoryId != null) {
            viewModelState.subCategories
                .firstOrNull { it.id == viewModelState.selectedSubCategoryId }
                ?.name ?: allLabel
        } else {
            allLabel
        }

        return RootUsageHostScreenUiState.CategoryFilterState(
            categoryDropdown = RootUsageHostScreenUiState.DropdownState(
                selectedLabel = selectedCategoryName,
                items = categoryItems,
            ),
            subCategoryDropdown = RootUsageHostScreenUiState.DropdownState(
                selectedLabel = selectedSubCategoryName,
                items = subCategoryItems,
            ),
        )
    }

    public val uiStateFlow: StateFlow<RootUsageHostScreenUiState> = MutableStateFlow(
        RootUsageHostScreenUiState(
            type = RootUsageHostScreenUiState.Type.Calendar,
            header = RootUsageHostScreenUiState.Header.None,
            textInputUiState = null,
            searchText = "",
            categoryFilterState = RootUsageHostScreenUiState.CategoryFilterState(
                categoryDropdown = RootUsageHostScreenUiState.DropdownState(
                    selectedLabel = "全て",
                    items = ImmutableList(listOf()),
                ),
                subCategoryDropdown = RootUsageHostScreenUiState.DropdownState(
                    selectedLabel = "全て",
                    items = ImmutableList(listOf()),
                ),
            ),
            event = event,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            mutableViewModelStateFlow
                .collectLatest { viewModelState ->
                    viewModelState.screenStructure ?: return@collectLatest

                    uiStateFlow.update {
                        it.copy(
                            type = when (viewModelState.screenStructure) {
                                is ScreenStructure.Root.Usage.Calendar -> RootUsageHostScreenUiState.Type.Calendar
                                is ScreenStructure.Root.Usage.List -> RootUsageHostScreenUiState.Type.List
                            },
                            header = when (viewModelState.screenStructure) {
                                is ScreenStructure.Root.Usage.Calendar -> run {
                                    RootUsageHostScreenUiState.Header.Calendar(
                                        title = viewModelState.calendarTitle ?: return@run RootUsageHostScreenUiState.Header.None,
                                        year = viewModelState.calendarYear ?: return@run RootUsageHostScreenUiState.Header.None,
                                        month = viewModelState.calendarMonth ?: return@run RootUsageHostScreenUiState.Header.None,
                                        event = viewModelState.calendarEvent ?: return@run RootUsageHostScreenUiState.Header.None,
                                    )
                                }

                                is ScreenStructure.Root.Usage.List -> RootUsageHostScreenUiState.Header.None
                            },
                            textInputUiState = viewModelState.textInputUiState,
                            searchText = viewModelState.searchText,
                            categoryFilterState = createCategoryFilterState(viewModelState),
                        )
                    }
                }
        }
    }.asStateFlow()

    public fun updateEventListener(event: RootUsageHostScreenUiState.HeaderCalendarEvent) {
        mutableViewModelStateFlow.update {
            it.copy(
                calendarEvent = event,
            )
        }
    }

    public fun updateHeaderTitle(title: String) {
        mutableViewModelStateFlow.update {
            it.copy(
                calendarTitle = title,
            )
        }
    }

    public fun updateCalendarYearMonth(year: Int, month: Int) {
        mutableViewModelStateFlow.update {
            it.copy(
                calendarYear = year,
                calendarMonth = month,
            )
        }
    }

    public fun updateStructure(structure: ScreenStructure.Root.Usage) {
        mutableViewModelStateFlow.update {
            it.copy(
                screenStructure = structure,
            )
        }
    }

    public fun requestNavigate() {
        viewModelScope.launch {
            rootNavigationEventSender.send {
                it.navigate(
                    mutableViewModelStateFlow.value.screenStructure
                        ?: ScreenStructure.Root.Usage.Calendar(),
                )
            }
        }
    }

    public interface RootNavigationEvent {
        public fun navigate(screenStructure: ScreenStructure)
    }

    public data class CategoryInfo(
        val id: MoneyUsageCategoryId,
        val name: String,
    )

    public data class SubCategoryInfo(
        val id: MoneyUsageSubCategoryId,
        val name: String,
    )

    public data class ViewModelState(
        val screenStructure: ScreenStructure.Root.Usage? = null,
        val calendarEvent: RootUsageHostScreenUiState.HeaderCalendarEvent? = null,
        val calendarTitle: String? = null,
        val calendarYear: Int? = null,
        val calendarMonth: Int? = null,
        val textInputUiState: RootUsageHostScreenUiState.TextInputUiState? = null,
        val searchText: String = "",
        val categories: List<CategoryInfo> = listOf(),
        val subCategories: List<SubCategoryInfo> = listOf(),
        val selectedCategoryId: MoneyUsageCategoryId? = null,
        val selectedSubCategoryId: MoneyUsageSubCategoryId? = null,
    )
}
