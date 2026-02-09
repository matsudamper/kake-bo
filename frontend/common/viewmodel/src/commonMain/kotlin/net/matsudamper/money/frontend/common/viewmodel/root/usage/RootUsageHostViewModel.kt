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
import net.matsudamper.money.frontend.graphql.type.MoneyUsageCategoriesInput

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

        override fun onClickCategoryFilterClear() {
            mutableViewModelStateFlow.update {
                it.copy(
                    selectedCategoryId = null,
                )
            }
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

    private fun createCategoryFilterState(viewModelState: ViewModelState): RootUsageHostScreenUiState.CategoryFilterState {
        val categories = viewModelState.categories.map { category ->
            RootUsageHostScreenUiState.CategoryItem(
                name = category.name,
                isSelected = category.id == viewModelState.selectedCategoryId,
                event = object : RootUsageHostScreenUiState.CategoryItemEvent {
                    override fun onClick() {
                        mutableViewModelStateFlow.update {
                            it.copy(
                                selectedCategoryId = category.id,
                            )
                        }
                    }
                },
            )
        }.toImmutableList()

        val selectedName = if (viewModelState.selectedCategoryId != null) {
            viewModelState.categories
                .firstOrNull { it.id == viewModelState.selectedCategoryId }
                ?.name
        } else {
            null
        }

        return RootUsageHostScreenUiState.CategoryFilterState(
            categories = categories,
            selectedCategoryName = selectedName,
        )
    }

    public val uiStateFlow: StateFlow<RootUsageHostScreenUiState> = MutableStateFlow(
        RootUsageHostScreenUiState(
            type = RootUsageHostScreenUiState.Type.Calendar,
            header = RootUsageHostScreenUiState.Header.None,
            textInputUiState = null,
            searchText = "",
            categoryFilterState = RootUsageHostScreenUiState.CategoryFilterState(
                categories = ImmutableList(listOf()),
                selectedCategoryName = null,
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

    public data class ViewModelState(
        val screenStructure: ScreenStructure.Root.Usage? = null,
        val calendarEvent: RootUsageHostScreenUiState.HeaderCalendarEvent? = null,
        val calendarTitle: String? = null,
        val textInputUiState: RootUsageHostScreenUiState.TextInputUiState? = null,
        val searchText: String = "",
        val categories: List<CategoryInfo> = listOf(),
        val selectedCategoryId: MoneyUsageCategoryId? = null,
    )
}
