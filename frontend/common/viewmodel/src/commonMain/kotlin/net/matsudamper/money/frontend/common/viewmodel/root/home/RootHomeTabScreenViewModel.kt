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
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByDateQuery

public class RootHomeTabScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
    private val loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val reservedColorModel = ReservedColorModel()

    private val uiStateEvent = object : RootHomeTabUiState.Event {
        override fun onViewInitialized() {
            fetch()
        }
    }

    private val betweenEvent = object : RootHomeTabUiState.BetweenEvent {
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
    }

    public val uiStateFlow: StateFlow<RootHomeTabUiState> = MutableStateFlow(
        RootHomeTabUiState(
            contentType = RootHomeTabUiState.ContentType.Between(
                loadingState = RootHomeTabUiState.BetweenLoadingState.Loading,
                event = betweenEvent,
            ),
            event = uiStateEvent,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val screenState = when (viewModelState.contentTYpe) {
                    ViewModelState.ContentType.Between -> run screenState@{
                        val loadingState = RootHomeTabUiState.ContentType.Between(
                            loadingState = RootHomeTabUiState.BetweenLoadingState.Loading,
                            event = betweenEvent,
                        )
                        val errorState = RootHomeTabUiState.ContentType.Between(
                            loadingState = RootHomeTabUiState.BetweenLoadingState.Error,
                            event = betweenEvent,
                        )

                        val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                            viewModelState.displayPeriod.sinceDate.addMonth(index)
                        }
                        val allLoaded = displayPeriods.all { displayPeriod ->
                            viewModelState.responseMap.contains(displayPeriod)
                        }
                        if (allLoaded.not()) {
                            return@screenState loadingState
                        }

                        val responses = run {
                            val responses = displayPeriods.map { displayPeriod ->
                                viewModelState.responseMap[displayPeriod]
                            }
                            if (responses.size != responses.filterNotNull().size) {
                                return@screenState errorState
                            }
                            displayPeriods.zip(responses.filterNotNull())
                        }

                        RootHomeTabUiState.ContentType.Between(
                            event = betweenEvent,
                            loadingState = RootHomeTabUiState.BetweenLoadingState.Loaded(
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
                                                        ?: return@screenState errorState

                                                byCategory.map {
                                                    val amount = it.totalAmount
                                                        ?: return@screenState errorState
                                                    BarGraphUiState.Item(
                                                        color = reservedColorModel.getColor(it.category.id.id.toString()),
                                                        title = it.category.name,
                                                        value = amount,
                                                    )
                                                }.toImmutableList()
                                            },
                                            total = response.data?.user?.moneyUsageAnalytics?.totalAmount
                                                ?: return@screenState errorState,
                                        )
                                    }.toImmutableList(),
                                ),
                                totalBarColorTextMapping = responses.mapNotNull { (_, response) ->
                                    response.data?.user?.moneyUsageAnalytics?.byCategories
                                }.flatMap { byCategories ->
                                    byCategories.map { it.category }
                                }.distinctBy {
                                    it.id
                                }.map {
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
                                            ?: return@screenState errorState,
                                        year = yearMonth.year,
                                        month = yearMonth.month,
                                    )
                                }.toImmutableList(),
                            ),
                        )
                    }
                }
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        contentType = screenState,
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            loginCheckUseCase.check()
        }
    }

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

    public interface Event {
        public fun navigateToMailImport()
        public fun navigateToMailLink()
    }

    private data class ViewModelState(
        val contentTYpe: ContentType = ContentType.Between,
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

        enum class ContentType {
            Between,
        }
    }
}
