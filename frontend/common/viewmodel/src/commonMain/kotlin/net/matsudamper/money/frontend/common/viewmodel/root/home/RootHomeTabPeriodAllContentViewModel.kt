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
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAllContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabUiState
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByDateQuery

public class RootHomeTabPeriodAllContentViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(ViewModelState())
    private val reservedColorModel = ReservedColorModel()

    public val uiStateFlow: StateFlow<RootHomeTabPeriodAllContentUiState> = MutableStateFlow(
        RootHomeTabPeriodAllContentUiState(
            loadingState = RootHomeTabPeriodAllContentUiState.LoadingState.Loading,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
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

    init {
        coroutineScope.launch {
            val period = viewModelStateFlow.value.displayPeriod

            fetchAll(
                period = period.copy(
                    sinceDate = period.sinceDate.addMonth(1),
                ),
                forceReFetch = false,
            )
        }
    }

    private fun fetchAll(period: ViewModelState.Period, forceReFetch: Boolean) {
        coroutineScope.launch {
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
                        val result = api.fetchAll(
                            startYear = startYearMonth.year,
                            startMonth = startYearMonth.month,
                            endYear = endYearMonth.year,
                            endMonth = endYearMonth.month,
                            useCache = forceReFetch.not(),
                        )

                        viewModelStateFlow.update {
                            it.copy(
                                allResponseMap = it.allResponseMap
                                    .plus(startYearMonth to result.getOrNull()),
                            )
                        }
                    }
                }.map { it.await() }
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
            barGraph = BarGraphUiState(
                items = responses.map { (yearMonth, response) ->
                    BarGraphUiState.PeriodData(
                        year = yearMonth.year,
                        month = yearMonth.month,
                        items = run item@{
                            val byCategory =
                                response.data?.user?.moneyUsageAnalytics?.byCategories
                                    ?: return null

                            byCategory.map {
                                val amount = it.totalAmount
                                    ?: return null
                                BarGraphUiState.Item(
                                    color = reservedColorModel.getColor(it.category.id.value.toString()),
                                    title = it.category.name,
                                    value = amount,
                                )
                            }.toImmutableList()
                        },
                        total = response.data?.user?.moneyUsageAnalytics?.totalAmount
                            ?: return null,
                    )
                }.toImmutableList(),
            ),
            totalBarColorTextMapping = categories.map {
                RootHomeTabUiState.ColorText(
                    color = reservedColorModel.getColor(it.id.value.toString()),
                    text = it.name,
                    onClick = {
//                        viewModelStateFlow.update { viewModelState ->
//                            viewModelState.copy(
//                                contentType = ViewModelState.ContentType.Category(
//                                    categoryId = it.id,
//                                    name = it.name,
//                                ),
//                            )
//                        }
//
//                        fetch(
//                            period = viewModelStateFlow.value.displayPeriod,
//                        )
                    },
                )
            }.toImmutableList(),
            monthTotalItems = responses.map { (yearMonth, response) ->
                RootHomeTabPeriodContentUiState.MonthTotalItem(
                    amount = Formatter.formatMoney(
                        response.data?.user?.moneyUsageAnalytics?.totalAmount
                            ?: return null,
                    ) + "円",
                    title = "${yearMonth.year}/${yearMonth.month.toString().padStart(2, '0')}",
                )
            }.toImmutableList(),
        )
    }

    public fun updateStructure(current: RootHomeScreenStructure.Period) {
        val since = current.since ?: return // TODO
        viewModelStateFlow.update {
            it.copy(
                displayPeriod = it.displayPeriod.copy(
                    sinceDate = ViewModelState.YearMonth(since.year, since.monthNumber),
                ),
            )
        }
        fetchAll(
            period = viewModelStateFlow.value.displayPeriod,
            forceReFetch = false,
        )
    }


    private data class ViewModelState(
        val allResponseMap: Map<YearMonth, ApolloResponse<RootHomeTabScreenAnalyticsByDateQuery.Data>?> = mapOf(),
        val displayPeriod: Period = run {
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
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
