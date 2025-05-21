package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.GraphTitleChipUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAndCategoryUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodSubCategoryContentUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.PlatformType
import net.matsudamper.money.frontend.common.viewmodel.PlatformTypeProvider
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EqualsImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByCategoryQuery

public class RootHomeTabPeriodSubCategoryContentViewModel(
    initialCategoryId: MoneyUsageCategoryId,
    initialSubCategoryId: Int,
    scopedObjectFeature: ScopedObjectFeature,
    private val api: RootHomeTabScreenApi,
    graphqlClient: GraphqlClient,
    loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val reservedColorModel = ReservedColorModel()
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            categoryId = initialCategoryId,
            subCategoryId = initialSubCategoryId
        )
    )

    private val tabViewModel = RootHomeTabScreenViewModel(
        scopedObjectFeature = scopedObjectFeature,
        loginCheckUseCase = loginCheckUseCase,
    ).also { viewModel ->
        viewModelScope.launch {
            viewModel.viewModelEventHandler.collect(
                object : RootHomeTabScreenViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        viewModelScope.launch { eventSender.send { it.navigate(screen) } }
                    }
                },
            )
        }
    }
    private val periodViewModel = RootHomeTabPeriodScreenViewModel(
        scopedObjectFeature = scopedObjectFeature,
        api = RootHomeTabScreenApi(graphqlClient = graphqlClient),
        initialCategoryId = initialCategoryId,
    ).also { viewModel ->
        viewModelScope.launch {
            viewModel.viewModelEventHandler.collect(
                object : RootHomeTabPeriodScreenViewModel.Event {
                    override fun onClickAllFilter() {
                        viewModelScope.launch {
                            eventSender.send {
                                it.navigate(
                                    RootHomeScreenStructure.PeriodAnalytics(
                                        since = viewModel.getCurrentLocalDate(),
                                        period = viewModelStateFlow.value.displayPeriod.monthCount,
                                    ),
                                )
                            }
                        }
                    }

                    override fun onClickCategoryFilter(categoryId: MoneyUsageCategoryId) {
                        viewModelScope.launch {
                            eventSender.send {
                                it.navigate(
                                    RootHomeScreenStructure.PeriodCategory(
                                        categoryId = categoryId,
                                        since = viewModel.getCurrentLocalDate(),
                                        period = viewModelStateFlow.value.displayPeriod.monthCount,
                                    ),
                                )
                            }
                        }
                    }

                    override fun updateSinceDate(
                        year: Int,
                        month: Int,
                        period: Int,
                    ) {
                        viewModelScope.launch {
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    displayPeriod = viewModelState.displayPeriod.copy(
                                        sinceDate = ViewModelState.YearMonth(
                                            year = year,
                                            month = month,
                                        ),
                                        monthCount = period,
                                    ),
                                )
                            }
                        }
                    }
                },
            )
        }
    }

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiStateFlow: StateFlow<RootHomeTabPeriodSubCategoryContentUiState> = MutableStateFlow(
        RootHomeTabPeriodSubCategoryContentUiState(
            loadingState = RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading,
            rootHomeTabPeriodAndCategoryUiState = periodViewModel.uiStateFlow.value,
            rootHomeTabUiState = tabViewModel.uiStateFlow.value,
            rootScaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
                override fun onClickHome() {
                    if (PlatformTypeProvider.type == PlatformType.JS) {
                        super.onClickHome()
                    }
                }
            },
            event = object : RootHomeTabPeriodSubCategoryContentUiState.Event {
                override suspend fun onViewInitialized() {
                    fetchCategory(
                        period = viewModelStateFlow.value.displayPeriod,
                        categoryId = initialCategoryId,
                    )
                }

                override fun refresh() {
                    fetchCategory(
                        period = viewModelStateFlow.value.displayPeriod,
                        categoryId = initialCategoryId,
                        forceReFetch = true,
                    )
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            tabViewModel.uiStateFlow.collectLatest { tabUiState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        rootHomeTabUiState = tabUiState,
                    )
                }
            }
        }
        viewModelScope.launch {
            periodViewModel.uiStateFlow.collectLatest { periodUiState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        rootHomeTabPeriodAndCategoryUiState = periodUiState,
                    )
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                    viewModelState.displayPeriod.sinceDate.addMonth(index)
                }
                uiStateFlow.update {
                    it.copy(
                        loadingState = createSubCategoryUiState(
                            categoryId = viewModelState.categoryId,
                            subCategoryId = viewModelState.subCategoryId,
                            displayPeriods = displayPeriods,
                            categoryResponseMap = viewModelState.categoryResponseMap,
                        ),
                    )
                }
            }
        }
    }.asStateFlow()

    public fun updateStructure(current: RootHomeScreenStructure.PeriodSubCategory) {
        val since = current.since

        periodViewModel.updateScreenStructure(current)
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                categoryId = current.categoryId,
                subCategoryId = current.subCategoryId,
                displayPeriod = viewModelState.displayPeriod.copy(
                    sinceDate = run since@{
                        if (since != null) {
                            ViewModelState.YearMonth(
                                year = since.year,
                                month = since.monthNumber,
                            )
                        } else {
                            viewModelState.displayPeriod.sinceDate
                        }
                    },
                ),
            )
        }
        fetchCategory(
            period = viewModelStateFlow.value.displayPeriod,
            categoryId = current.categoryId,
            forceReFetch = false,
        )
    }

    private fun createSubCategoryUiState(
        categoryId: MoneyUsageCategoryId,
        subCategoryId: Int,
        displayPeriods: List<ViewModelState.YearMonth>,
        categoryResponseMap: Map<ViewModelState.YearMonthCategory, ApolloResponse<RootHomeTabScreenAnalyticsByCategoryQuery.Data>?>,
    ): RootHomeTabPeriodSubCategoryContentUiState.LoadingState {
        val loadingState = run loadingState@{
            RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loaded(
                graphItems = BarGraphUiState(
                    items = displayPeriods.map { yearMonth ->
                        val key = ViewModelState.YearMonthCategory(
                            categoryId = categoryId,
                            yearMonth = yearMonth,
                        )
                        val apolloResult = categoryResponseMap[key]
                            ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading

                        val subCategory = apolloResult.data?.user?.moneyUsageAnalyticsByCategory?.bySubCategories
                            ?.firstOrNull { it.subCategory.id.toString() == subCategoryId.toString() }
                            ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error

                        val amount = subCategory.totalAmount
                            ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error

                        BarGraphUiState.PeriodData(
                            year = yearMonth.year,
                            month = yearMonth.month,
                            items = listOf(
                                BarGraphUiState.Item(
                                    color = reservedColorModel.getColor(subCategory.subCategory.id.toString()),
                                    title = subCategory.subCategory.name,
                                    value = amount,
                                )
                            ).toImmutableList(),
                            total = amount,
                            event = object : BarGraphUiState.PeriodDataEvent {
                                override fun onClick() {
                                    viewModelScope.launch {
                                        eventSender.send {
                                            it.navigate(
                                                RootHomeScreenStructure.MonthlyCategory(
                                                    categoryId = categoryId,
                                                    year = yearMonth.year,
                                                    month = yearMonth.month,
                                                ),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }.toImmutableList(),
                ),
                graphTitleItems = displayPeriods.flatMap { yearMonth ->
                    val key = ViewModelState.YearMonthCategory(
                        categoryId = categoryId,
                        yearMonth = yearMonth,
                    )
                    val apolloResult = categoryResponseMap[key] ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading
                    val subCategory = apolloResult.data?.user?.moneyUsageAnalyticsByCategory?.bySubCategories
                        ?.firstOrNull { it.subCategory.id.toString() == subCategoryId.toString() }
                        ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error

                    listOf(subCategory.subCategory)
                }.distinctBy { it.id }
                    .map {
                        GraphTitleChipUiState(
                            title = it.name,
                            color = reservedColorModel.getColor(it.id.toString()),
                            onClick = {},
                        )
                    }.toImmutableList(),
                monthTotalItems = displayPeriods.map { yearMonth ->
                    val key = ViewModelState.YearMonthCategory(
                        categoryId = categoryId,
                        yearMonth = yearMonth,
                    )

                    val apolloResult = categoryResponseMap[key] ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading
                    val subCategory = apolloResult.data?.user?.moneyUsageAnalyticsByCategory?.bySubCategories
                        ?.firstOrNull { it.subCategory.id.toString() == subCategoryId.toString() }
                        ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error

                    val amount = subCategory.totalAmount ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error

                    RootHomeTabPeriodAndCategoryUiState.MonthTotalItem(
                        title = "${yearMonth.year}/${yearMonth.month.toString().padStart(2, '0')}",
                        amount = Formatter.formatMoney(amount) + "円",
                        event = MonthTotalItemEvent(yearMonth),
                    )
                }.toImmutableList(),
                subCategoryName = run {
                    val key = ViewModelState.YearMonthCategory(
                        categoryId = categoryId,
                        yearMonth = displayPeriods.firstOrNull() ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error,
                    )
                    val apolloResult = categoryResponseMap[key] ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading
                    val subCategory = apolloResult.data?.user?.moneyUsageAnalyticsByCategory?.bySubCategories
                        ?.firstOrNull { it.subCategory.id.toString() == subCategoryId.toString() }
                        ?: return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error

                    subCategory.subCategory.name
                }
            )
        }

        return loadingState
    }

    private inner class MonthTotalItemEvent(
        private val yearMonth: ViewModelState.YearMonth,
    ) : RootHomeTabPeriodAndCategoryUiState.MonthTotalItem.Event, EqualsImpl(yearMonth) {
        override fun onClick() {
            viewModelScope.launch {
                eventSender.send {
                    it.navigate(
                        RootHomeScreenStructure.Monthly(
                            date = LocalDate(yearMonth.year, yearMonth.month, 1),
                        ),
                    )
                }
            }
        }
    }

    private fun fetchCategory(
        period: ViewModelState.Period,
        categoryId: MoneyUsageCategoryId,
        forceReFetch: Boolean = false,
    ) {
        viewModelScope.launch {
            (0 until period.monthCount)
                .map { index ->
                    val start = period.sinceDate.addMonth(index)

                    start to start.addMonth(1)
                }
                .filter { (startYearMonth, _) ->
                    forceReFetch || viewModelStateFlow.value.categoryResponseMap[
                        ViewModelState.YearMonthCategory(
                            categoryId = categoryId,
                            yearMonth = startYearMonth,
                        ),
                    ] == null
                }
                .map { (startYearMonth, endYearMonth) ->
                    val key = ViewModelState.YearMonthCategory(
                        categoryId = categoryId,
                        yearMonth = startYearMonth,
                    )
                    async {
                        val result = api.fetchCategory(
                            id = categoryId,
                            startYear = startYearMonth.year,
                            startMonth = startYearMonth.month,
                            endYear = endYearMonth.year,
                            endMonth = endYearMonth.month,
                            useCache = forceReFetch.not(),
                        )

                        viewModelStateFlow.update {
                            it.copy(
                                categoryResponseMap = it.categoryResponseMap
                                    .plus(key to result.getOrNull()),
                            )
                        }
                    }
                }.map { it.await() }

            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = period,
                )
            }
        }
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val categoryId: MoneyUsageCategoryId,
        val subCategoryId: Int,
        val categoryResponseMap: Map<YearMonthCategory, ApolloResponse<RootHomeTabScreenAnalyticsByCategoryQuery.Data>?> = mapOf(),
        val displayPeriod: Period = run {
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            Period(
                sinceDate = YearMonth(currentDate.year, currentDate.monthNumber).addMonth(-5),
                monthCount = 6,
            )
        },
    ) {
        data class YearMonthCategory(
            val categoryId: MoneyUsageCategoryId,
            val yearMonth: YearMonth,
        )

        data class YearMonth(
            val year: Int,
            val month: Int,
        ) {
            fun addMonth(count: Int): YearMonth {
                val nextDate = LocalDate(year, month, 1)
                    .plus(count, DateTimeUnit.MONTH)

                return YearMonth(
                    year = nextDate.year,
                    month = nextDate.monthNumber,
                )
            }
        }

        data class Period(
            val sinceDate: YearMonth,
            val monthCount: Int,
        )
    }
}
