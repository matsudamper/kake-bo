package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.GraphTitleChipUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAllContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.ViewModelFeature
import net.matsudamper.money.frontend.common.viewmodel.lib.EqualsImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByDateQuery

public class RootHomeTabPeriodAllContentViewModel(
    viewModelFeature: ViewModelFeature,
    private val api: RootHomeTabScreenApi,
    graphqlClient: GraphqlClient,
    loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
) : CommonViewModel(viewModelFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(ViewModelState())
    private val reservedColorModel = ReservedColorModel()

    private val tabViewModel = RootHomeTabScreenViewModel(
        viewModelFeature = viewModelFeature,
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
        viewModelFeature = viewModelFeature,
        api = RootHomeTabScreenApi(graphqlClient = graphqlClient),
        initialCategoryId = null,
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
                            eventSender.send {
                                it.navigate(
                                    RootHomeScreenStructure.PeriodAnalytics(
                                        since = LocalDate(year, month, 1),
                                        period = period,
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

    public val uiStateFlow: StateFlow<RootHomeTabPeriodAllContentUiState> = MutableStateFlow(
        RootHomeTabPeriodAllContentUiState(
            loadingState = RootHomeTabPeriodAllContentUiState.LoadingState.Loading,
            rootHomeTabPeriodUiState = periodViewModel.uiStateFlow.value,
            rootHomeTabUiState = tabViewModel.uiStateFlow.value,
            event = object : RootHomeTabPeriodAllContentUiState.Event {
                override suspend fun onViewInitialized() {
                    val period = viewModelStateFlow.value.displayPeriod

                    fetchAll(
                        period = period,
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
                        rootHomeTabPeriodUiState = periodUiState,
                    )
                }
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                    viewModelState.displayPeriod.sinceDate.addMonth(index)
                }
                val screenState = run screenState@{
                    val allLoaded = displayPeriods.all { displayPeriod ->
                        viewModelState.allResponseMap.contains(displayPeriod)
                    }
                    if (allLoaded.not()) {
                        return@screenState RootHomeTabPeriodAllContentUiState.LoadingState.Loading
                    }

                    val responses = run {
                        val responses = displayPeriods.map { displayPeriod ->
                            viewModelState.allResponseMap[displayPeriod]
                        }
                        if (responses.size != responses.filterNotNull().size) {
                            return@screenState RootHomeTabPeriodAllContentUiState.LoadingState.Error
                        }
                        displayPeriods.zip(responses.filterNotNull())
                    }

                    val categories = responses.mapNotNull { (_, response) ->
                        response.data?.user?.moneyUsageAnalytics?.byCategories
                    }.flatMap { byCategories ->
                        byCategories.map { it.category }
                    }.distinctBy {
                        it.id
                    }

                    createTotalUiState(
                        responses = responses,
                        categories = categories,
                    ) ?: return@screenState RootHomeTabPeriodAllContentUiState.LoadingState.Error
                }

                uiStateFlow.update {
                    it.copy(
                        loadingState = screenState,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun fetchAll(
        period: ViewModelState.Period,
        forceReFetch: Boolean,
    ) {
        viewModelScope.launch {
            (0 until period.monthCount)
                .map { index ->
                    val start = period.sinceDate.addMonth(index)

                    start to start.addMonth(1)
                }
                .filter { (startYearMonth, _) ->
                    forceReFetch || viewModelStateFlow.value.allResponseMap.contains(startYearMonth).not()
                }
                .map { (startYearMonth, endYearMonth) ->
                    async {
                        val flowResult = api.fetchAll(
                            startYear = startYearMonth.year,
                            startMonth = startYearMonth.month,
                            endYear = endYearMonth.year,
                            endMonth = endYearMonth.month,
                            useCache = forceReFetch.not(),
                        )
                        flowResult
                            .onSuccess { flow ->
                                flow.collectLatest { apolloResponse ->
                                    if (apolloResponse.data == null) {
                                        return@collectLatest
                                    }
                                    viewModelStateFlow.update {
                                        it.copy(
                                            allResponseMap = it.allResponseMap
                                                .plus(startYearMonth to apolloResponse),
                                        )
                                    }
                                }
                            }.onFailure {
                                Logger.d("LOG", "Error: ${it.message}")
                                viewModelStateFlow.update {
                                    it.copy(
                                        allResponseMap = it.allResponseMap
                                            .plus(startYearMonth to null),
                                    )
                                }
                            }
                    }
                }.awaitAll()
            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = period,
                )
            }
//            updateUrl()
        }
    }

    private fun createTotalUiState(
        responses: List<Pair<ViewModelState.YearMonth, ApolloResponse<RootHomeTabScreenAnalyticsByDateQuery.Data>>>,
        categories: List<RootHomeTabScreenAnalyticsByDateQuery.Category>,
    ): RootHomeTabPeriodAllContentUiState.LoadingState.Loaded? {
        return RootHomeTabPeriodAllContentUiState.LoadingState.Loaded(
            barGraph =
            BarGraphUiState(
                items =
                responses.map { (yearMonth, response) ->
                    BarGraphUiState.PeriodData(
                        year = yearMonth.year,
                        month = yearMonth.month,
                        items =
                        run item@{
                            val byCategory =
                                response.data?.user?.moneyUsageAnalytics?.byCategories
                                    ?: return null

                            byCategory.map {
                                val amount =
                                    it.totalAmount
                                        ?: return null
                                BarGraphUiState.Item(
                                    color = reservedColorModel.getColor(it.category.id.value.toString()),
                                    title = it.category.name,
                                    value = amount,
                                )
                            }.toImmutableList()
                        },
                        total =
                        response.data?.user?.moneyUsageAnalytics?.totalAmount
                            ?: return null,
                        event =
                        object : BarGraphUiState.PeriodDataEvent {
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
                        },
                    )
                }.toImmutableList(),
            ),
            totalBarColorTextMapping =
            categories.map { category ->
                GraphTitleChipUiState(
                    color = reservedColorModel.getColor(category.id.value.toString()),
                    title = category.name,
                    onClick = {
                        viewModelScope.launch {
                            eventSender.send {
                                it.navigate(RootHomeScreenStructure.PeriodCategory(category.id))
                            }
                        }
                    },
                )
            }.toImmutableList(),
            monthTotalItems =
            responses.map { (yearMonth, response) ->
                RootHomeTabPeriodUiState.MonthTotalItem(
                    amount =
                    Formatter.formatMoney(
                        response.data?.user?.moneyUsageAnalytics?.totalAmount
                            ?: return null,
                    ) + "円",
                    title = "${yearMonth.year}/${yearMonth.month.toString().padStart(2, '0')}",
                    event = MonthTotalItemEvent(yearMonth),
                )
            }.toImmutableList(),
        )
    }

    private inner class MonthTotalItemEvent(
        private val yearMonth: ViewModelState.YearMonth,
    ) : RootHomeTabPeriodUiState.MonthTotalItem.Event, EqualsImpl(yearMonth) {
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

    public fun updateStructure(current: RootHomeScreenStructure.Period) {
        val since = current.since
        if (since != null) {
            viewModelStateFlow.update {
                it.copy(
                    displayPeriod =
                    it.displayPeriod.copy(
                        sinceDate = ViewModelState.YearMonth(since.year, since.monthNumber),
                        monthCount = current.period,
                    ),
                )
            }
        }
        fetchAll(
            period = viewModelStateFlow.value.displayPeriod,
            forceReFetch = false,
        )
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val allResponseMap: Map<YearMonth, ApolloResponse<RootHomeTabScreenAnalyticsByDateQuery.Data>?> = mapOf(),
        val displayPeriod: Period =
            run {
                val currentDate =
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                Period(
                    sinceDate = YearMonth(currentDate.year, currentDate.monthNumber).addMonth(-5),
                    monthCount = 6,
                )
            },
    ) {
        data class YearMonth(
            val year: Int,
            val month: Int,
        ) {
            fun addMonth(count: Int): YearMonth {
                val nextDate =
                    LocalDate(year, month, 1)
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
