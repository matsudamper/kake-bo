package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.layout.graph.PolygonalLineGraphItemUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabUiState
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByCategoryQuery
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByDateQuery
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenQuery

public class RootHomeTabPeriodScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val reservedColorModel = ReservedColorModel()

    private val event = object : RootHomeTabPeriodContentUiState.Event {
        override fun onClickNextMonth() {
            val period = viewModelStateFlow.value.displayPeriod

            fetch(
                period = period.copy(
                    sinceDate = period.sinceDate.addMonth(1),
                ),
            )
        }

        override fun onClickPreviousMonth() {
            val period = viewModelStateFlow.value.displayPeriod

            fetch(
                period = period.copy(
                    sinceDate = period.sinceDate.addMonth(-1),
                ),
            )
        }

        override fun onClickRange(range: Int) {
            val currentEndYearMonth = viewModelStateFlow.value.displayPeriod.let { period ->
                period.sinceDate.addMonth(period.monthCount)
            }
            val newSinceDate = currentEndYearMonth
                .addMonth(-range)

            fetch(
                period = ViewModelState.Period(
                    sinceDate = newSinceDate,
                    monthCount = range,
                ),
            )
        }

        override fun onViewInitialized() {
            fetch(viewModelStateFlow.value.displayPeriod)
        }

        override fun onClickRetry() {
            fetch(
                period = viewModelStateFlow.value.displayPeriod,
                forceReFetch = true,
            )
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

                    RootHomeTabPeriodContentUiState.LoadingState.Loaded(
                        categoryType = when (viewModelState.contentType) {
                            is ViewModelState.ContentType.All -> "すべて"
                            is ViewModelState.ContentType.Category -> viewModelState.contentType.name
                        },
                        categoryTypes = createCategoryTypes(categories = viewModelState.categories).toImmutableList(),
                        between = "${displayPeriods.first().year}/${displayPeriods.first().month} - ${displayPeriods.last().year}/${displayPeriods.last().month}",
                        rangeText = "${viewModelState.displayPeriod.monthCount}ヶ月",
                        graphContent = when (viewModelState.contentType) {
                            is ViewModelState.ContentType.All -> {
                                val allLoaded = displayPeriods.all { displayPeriod ->
                                    viewModelState.allResponseMap.contains(displayPeriod)
                                }
                                if (allLoaded.not()) {
                                    return@screenState RootHomeTabPeriodContentUiState.LoadingState.Loading
                                }

                                val responses = run {
                                    val responses = displayPeriods.map { displayPeriod ->
                                        viewModelState.allResponseMap[displayPeriod]
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

                                createTotalUiState(
                                    responses = responses,
                                    categories = categories,
                                ) ?: return@screenState RootHomeTabPeriodContentUiState.LoadingState.Error
                            }

                            is ViewModelState.ContentType.Category -> {
                                createCategoryUiState(
                                    categoryId = viewModelState.contentType.categoryId,
                                    displayPeriods = displayPeriods,
                                    categoryResponseMap = viewModelState.categoryResponseMap,
                                ) ?: return@screenState RootHomeTabPeriodContentUiState.LoadingState.Loading
                            }
                        },
                    )
                }

                uiStateFlow.value = RootHomeTabPeriodContentUiState(
                    loadingState = loadingState,
                    event = event,
                )
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            api.screenFlow().collectLatest {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        categories = it.data?.user?.moneyUsageCategories?.nodes.orEmpty(),
                    )
                }
            }
        }
    }

    public fun updateScreenStructure(current: ScreenStructure.Root.Home) {
        val since = current.since
        if (since != null) {
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
        }
    }

    private fun createCategoryTypes(
        categories: List<RootHomeTabScreenQuery.Node>,
    ): List<RootHomeTabPeriodContentUiState.CategoryTypes> {
        return buildList {
            add(
                RootHomeTabPeriodContentUiState.CategoryTypes(
                    title = "すべて",
                    onClick = {
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                contentType = ViewModelState.ContentType.All,
                            )
                        }
                    },
                ),
            )
            addAll(
                categories.map { category ->
                    RootHomeTabPeriodContentUiState.CategoryTypes(
                        title = category.name,
                        onClick = {
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    contentType = ViewModelState.ContentType.Category(
                                        categoryId = category.id,
                                        name = category.name,
                                    ),
                                )
                            }
                            fetch(
                                period = viewModelStateFlow.value.displayPeriod,
                            )
                        },
                    )
                },
            )
        }
    }

    private fun createCategoryUiState(
        categoryId: MoneyUsageCategoryId,
        displayPeriods: List<ViewModelState.YearMonth>,
        categoryResponseMap: Map<ViewModelState.YearMonthCategory, ApolloResponse<RootHomeTabScreenAnalyticsByCategoryQuery.Data>?>,
    ): RootHomeTabPeriodContentUiState.GraphContent.Category? {
        return RootHomeTabPeriodContentUiState.GraphContent.Category(
            graphItems = displayPeriods.map { yearMonth ->
                val key = ViewModelState.YearMonthCategory(
                    categoryId = categoryId,
                    yearMonth = yearMonth,
                )
                val apolloResult = categoryResponseMap[key] ?: return null
                val amount = apolloResult.data?.user?.moneyUsageAnalyticsByCategory?.totalAmount ?: return null

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

                val apolloResult = categoryResponseMap[key] ?: return null
                val result = apolloResult.data?.user?.moneyUsageAnalyticsByCategory ?: return null

                RootHomeTabPeriodContentUiState.MonthTotalItem(
                    title = "${yearMonth.year}/${yearMonth.month.toString().padStart(2, '0')}",
                    amount = Formatter.formatMoney(result.totalAmount ?: return null) + "円",
                )
            }.toImmutableList(),
        )
    }

    private fun createTotalUiState(
        responses: List<Pair<ViewModelState.YearMonth, ApolloResponse<RootHomeTabScreenAnalyticsByDateQuery.Data>>>,
        categories: List<RootHomeTabScreenAnalyticsByDateQuery.Category>,
    ): RootHomeTabPeriodContentUiState.GraphContent.Total? {
        return RootHomeTabPeriodContentUiState.GraphContent.Total(
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
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(
                                contentType = ViewModelState.ContentType.Category(
                                    categoryId = it.id,
                                    name = it.name,
                                ),
                            )
                        }

                        fetch(
                            period = viewModelStateFlow.value.displayPeriod,
                        )
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

    private fun fetch(
        period: ViewModelState.Period,
        forceReFetch: Boolean = false,
    ) {
        when (val type = viewModelStateFlow.value.contentType) {
            is ViewModelState.ContentType.All -> {
                fetchAll(
                    period = period,
                    forceReFetch = forceReFetch,
                )
            }

            is ViewModelState.ContentType.Category -> {
                fetchCategory(
                    period = period,
                    categoryId = type.categoryId,
                    forceReFetch = forceReFetch,
                )
            }
        }
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
            updateUrl()
        }
    }

    private fun updateUrl() {
        coroutineScope.launch {
            viewModelEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Home(
                        since = LocalDate(
                            viewModelStateFlow.value.displayPeriod.sinceDate.year,
                            viewModelStateFlow.value.displayPeriod.sinceDate.month,
                            1,
                        ),
                    ),
                )
            }
        }
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    private data class ViewModelState(
        val contentType: ContentType = ContentType.All,
        val allResponseMap: Map<YearMonth, ApolloResponse<RootHomeTabScreenAnalyticsByDateQuery.Data>?> = mapOf(),
        val categoryResponseMap: Map<YearMonthCategory, ApolloResponse<RootHomeTabScreenAnalyticsByCategoryQuery.Data>?> = mapOf(),
        val categories: List<RootHomeTabScreenQuery.Node> = listOf(),
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

        sealed interface ContentType {
            object All : ContentType
            data class Category(
                val categoryId: MoneyUsageCategoryId,
                val name: String,
            ) : ContentType
        }
    }
}
