package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
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
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.PolygonalLineGraphItemUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodCategoryContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByCategoryQuery

public class RootHomeTabPeriodCategoryContentViewModel(
    private val categoryId: MoneyUsageCategoryId,
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
    loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(ViewModelState(categoryId = categoryId))

    private val tabViewModel = RootHomeTabScreenViewModel(
        coroutineScope = coroutineScope,
        loginCheckUseCase = loginCheckUseCase,
    )
    private val periodViewModel = RootHomeTabPeriodScreenViewModel(
        coroutineScope = coroutineScope,
        api = RootHomeTabScreenApi(),
    )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()
    public val tabEventHandler: EventHandler<RootHomeTabScreenViewModel.Event> = tabViewModel.viewModelEventHandler
    public val periodEventHandler: EventHandler<RootHomeTabPeriodScreenViewModel.Event> = periodViewModel.viewModelEventHandler

    public val uiStateFlow: StateFlow<RootHomeTabPeriodCategoryContentUiState> = MutableStateFlow(
        RootHomeTabPeriodCategoryContentUiState(
            loadingState = RootHomeTabPeriodCategoryContentUiState.LoadingState.Loading,
            rootHomeTabPeriodUiState = periodViewModel.uiStateFlow.value,
            rootHomeTabUiState = tabViewModel.uiStateFlow.value,
            event = object : RootHomeTabPeriodCategoryContentUiState.Event {
                override suspend fun onViewInitialized() {
                    fetchCategory(
                        period = viewModelStateFlow.value.displayPeriod,
                        categoryId = categoryId,
                    )
                }
            },
        ),
    ).also { uiStateFlow ->
        tabViewModel.viewModelEventHandler
        coroutineScope.launch {
            tabViewModel.uiStateFlow.collectLatest { tabUiState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        rootHomeTabUiState = tabUiState,
                    )
                }
            }
        }
        coroutineScope.launch {
            periodViewModel.uiStateFlow.collectLatest { periodUiState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        rootHomeTabPeriodUiState = periodUiState,
                    )
                }
            }
        }
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                    viewModelState.displayPeriod.sinceDate.addMonth(index)
                }
                uiStateFlow.update {
                    it.copy(
                        loadingState = createCategoryUiState(
                            categoryId = viewModelState.categoryId,
                            displayPeriods = displayPeriods,
                            categoryResponseMap = viewModelState.categoryResponseMap,
                        ),
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createCategoryUiState(
        categoryId: MoneyUsageCategoryId,
        displayPeriods: List<ViewModelState.YearMonth>,
        categoryResponseMap: Map<ViewModelState.YearMonthCategory, ApolloResponse<RootHomeTabScreenAnalyticsByCategoryQuery.Data>?>,
    ): RootHomeTabPeriodCategoryContentUiState.LoadingState {
        val loadingState = run loadingState@{
            RootHomeTabPeriodCategoryContentUiState.LoadingState.Loaded(
                graphItems = displayPeriods.map { yearMonth ->
                    val key = ViewModelState.YearMonthCategory(
                        categoryId = categoryId,
                        yearMonth = yearMonth,
                    )
                    val apolloResult = categoryResponseMap[key] ?: return@loadingState RootHomeTabPeriodCategoryContentUiState.LoadingState.Loading
                    val amount = apolloResult.data?.user?.moneyUsageAnalyticsByCategory?.totalAmount ?: return@loadingState RootHomeTabPeriodCategoryContentUiState.LoadingState.Error

                    PolygonalLineGraphItemUiState(
                        year = yearMonth.year,
                        month = yearMonth.month,
                        amount = amount,
                    )
                }.toImmutableList(),
                monthTotalItems = displayPeriods.map { yearMonth ->
                    val key = ViewModelState.YearMonthCategory(
                        categoryId = categoryId,
                        yearMonth = yearMonth,
                    )

                    val apolloResult = categoryResponseMap[key] ?: return@loadingState RootHomeTabPeriodCategoryContentUiState.LoadingState.Loading
                    val result = apolloResult.data?.user?.moneyUsageAnalyticsByCategory ?: return@loadingState RootHomeTabPeriodCategoryContentUiState.LoadingState.Error

                    RootHomeTabPeriodUiState.MonthTotalItem(
                        title = "${yearMonth.year}/${yearMonth.month.toString().padStart(2, '0')}",
                        amount = Formatter.formatMoney(result.totalAmount ?: return@loadingState RootHomeTabPeriodCategoryContentUiState.LoadingState.Error) + "円",
                    )
                }.toImmutableList(),
            )
        }

        return loadingState
    }

    private fun fetchCategory(
        period: ViewModelState.Period,
        categoryId: MoneyUsageCategoryId,
        forceReFetch: Boolean = false,
    ) {
        coroutineScope.launch {
            (0 until period.monthCount)
                .map { index ->
                    val start = period.sinceDate.addMonth(index)

                    start to start.addMonth(1)
                }
                .filter { (startYearMonth, _) ->
                    forceReFetch || viewModelStateFlow.value.categoryResponseMap.contains(
                        ViewModelState.YearMonthCategory(
                            categoryId = categoryId,
                            yearMonth = startYearMonth,
                        ),
                    ).not()
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

            println(
                "fetch: ${
                (0 until period.monthCount)
                    .map { index ->
                        period.sinceDate.addMonth(index)
                    }.joinToString(postfix = "月") { it.month.toString() }
                }",
            )
            println("period: $period")
            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = period,
                )
            }
        }
    }

    public fun updateStructure(current: RootHomeScreenStructure.PeriodCategory) {
        val since = current.since ?: return
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                displayPeriod = viewModelState.displayPeriod.copy(
                    sinceDate = ViewModelState.YearMonth(
                        year = since.year,
                        month = since.monthNumber,
                    ),
                ),
            )
        }
        fetchCategory(
            period = viewModelStateFlow.value.displayPeriod,
            categoryId = categoryId,
            forceReFetch = false,
        )
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val categoryId: MoneyUsageCategoryId,
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
