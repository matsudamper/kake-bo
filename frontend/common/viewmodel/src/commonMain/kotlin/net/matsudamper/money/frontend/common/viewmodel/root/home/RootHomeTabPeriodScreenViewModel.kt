package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
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
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.layout.graph.PolygonalLineGraphItemUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabUiState
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByDateQuery

public class RootHomeTabPeriodScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val reservedColorModel = ReservedColorModel()

    private val event = object : RootHomeTabPeriodContentUiState.Event {
        override fun onClickNextMonth() {
            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = it.displayPeriod.copy(
                        sinceDate = it.displayPeriod.sinceDate.addMonth(1),
                    ),
                )
            }
            fetch()
        }

        override fun onClickPreviousMonth() {
            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = it.displayPeriod.copy(
                        sinceDate = it.displayPeriod.sinceDate.addMonth(-1),
                    ),
                )
            }
            fetch()
        }

        override fun onClickRange(range: Int) {
            viewModelStateFlow.update { viewModelState ->
                val currentEndYearMonth = viewModelState.displayPeriod.let { period ->
                    period.sinceDate.addMonth(period.monthCount)
                }
                val newSinceDate = currentEndYearMonth
                    .addMonth(-range)

                viewModelState.copy(
                    displayPeriod = ViewModelState.Period(
                        sinceDate = newSinceDate,
                        monthCount = range,
                    ),
                )
            }
            fetch()
        }

        override fun onViewInitialized() {
            fetch()
        }

        override fun onClickRetry() {
            fetch()
        }
    }

    public val uiStateFlow: StateFlow<RootHomeTabPeriodContentUiState> = MutableStateFlow(
        RootHomeTabPeriodContentUiState(
            loadingState = RootHomeTabPeriodContentUiState.LoadingState.Loading,
            event = event,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val loadingState = run screenState@{
                    val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                        viewModelState.displayPeriod.sinceDate.addMonth(index)
                    }
                    val allLoaded = displayPeriods.all { displayPeriod ->
                        viewModelState.responseMap.contains(displayPeriod)
                    }
                    if (allLoaded.not()) {
                        return@screenState RootHomeTabPeriodContentUiState.LoadingState.Loading
                    }

                    val responses = run {
                        val responses = displayPeriods.map { displayPeriod ->
                            viewModelState.responseMap[displayPeriod]
                        }
                        if (responses.size != responses.filterNotNull().size) {
                            return@screenState RootHomeTabPeriodContentUiState.LoadingState.Error
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

                    RootHomeTabPeriodContentUiState.LoadingState.Loaded(
                        categoryType = "すべて", // TODO
                        categoryTypes = buildList {
                            add(
                                RootHomeTabPeriodContentUiState.CategoryTypes(
                                    title = "すべて",
                                    onClick = {

                                    },
                                )
                            )
                            addAll(
                                categories.map {
                                    RootHomeTabPeriodContentUiState.CategoryTypes(
                                        title = it.name,
                                        onClick = {

                                        },
                                    )
                                }
                            )
                        }.toImmutableList(),
                        between = "${displayPeriods.first().year}/${displayPeriods.first().month} - ${displayPeriods.last().year}/${displayPeriods.last().month}",
                        rangeText = "${viewModelState.displayPeriod.monthCount}ヶ月",
                        totalBar = BarGraphUiState(
                            items = responses.map { (yearMonth, response) ->
                                BarGraphUiState.PeriodData(
                                    year = yearMonth.year,
                                    month = yearMonth.month,
                                    items = run item@{
                                        response.data?.user?.moneyUsageAnalytics?.byCategories.orEmpty()
                                            .forEach {
                                                println("${it.category.name} ${it.totalAmount}")
                                            }
                                        val byCategory =
                                            response.data?.user?.moneyUsageAnalytics?.byCategories
                                                ?: return@screenState RootHomeTabPeriodContentUiState.LoadingState.Error

                                        byCategory.map {
                                            val amount = it.totalAmount
                                                ?: return@screenState RootHomeTabPeriodContentUiState.LoadingState.Error
                                            BarGraphUiState.Item(
                                                color = reservedColorModel.getColor(it.category.id.id.toString()),
                                                title = it.category.name,
                                                value = amount,
                                            )
                                        }.toImmutableList()
                                    },
                                    total = response.data?.user?.moneyUsageAnalytics?.totalAmount
                                        ?: return@screenState RootHomeTabPeriodContentUiState.LoadingState.Error,
                                )
                            }.toImmutableList(),
                        ),
                        totalBarColorTextMapping = categories.map {
                            RootHomeTabUiState.ColorText(
                                color = reservedColorModel.getColor(it.id.id.toString()),
                                text = it.name,
                                onClick = {
                                },
                            )
                        }.toImmutableList(),
                        totals = responses.map { (yearMonth, response) ->
                            PolygonalLineGraphItemUiState(
                                amount = response.data?.user?.moneyUsageAnalytics?.totalAmount
                                    ?: return@screenState RootHomeTabPeriodContentUiState.LoadingState.Error,
                                year = yearMonth.year,
                                month = yearMonth.month,
                            )
                        }.toImmutableList(),
                    )
                }

                uiStateFlow.value = RootHomeTabPeriodContentUiState(
                    loadingState = loadingState,
                    event = event,
                )
            }
        }
    }.asStateFlow()

    private fun fetch() {
        coroutineScope.launch {
            val period = viewModelStateFlow.value.displayPeriod
            (0 until period.monthCount)
                .map { index ->
                    val start = period.sinceDate.addMonth(index)

                    start to start.addMonth(1)
                }
                .filter { (startYearMonth, _) ->
                    viewModelStateFlow.value.responseMap.contains(startYearMonth).not()
                }
                .map { (startYearMonth, endYearMonth) ->
                    launch {
                        val result = api.fetch(
                            startYear = startYearMonth.year,
                            startMonth = startYearMonth.month,
                            endYear = endYearMonth.year,
                            endMonth = endYearMonth.month,
                        )

                        viewModelStateFlow.update {
                            it.copy(
                                responseMap = it.responseMap
                                    .plus(startYearMonth to result.getOrNull()),
                            )
                        }
                    }
                }
        }
    }

    private data class ViewModelState(
        val responseMap: Map<YearMonth, ApolloResponse<RootHomeTabScreenAnalyticsByDateQuery.Data>?> = mapOf(),
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
