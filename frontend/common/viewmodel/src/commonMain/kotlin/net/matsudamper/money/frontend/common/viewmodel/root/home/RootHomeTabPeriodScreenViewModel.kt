package net.matsudamper.money.frontend.common.viewmodel.root.home

import com.apollographql.apollo3.api.ApolloResponse
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
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.layout.graph.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabUiState
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByDateQuery

public class RootHomeTabPeriodScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val reservedColorModel = ReservedColorModel()

    private val event = object : RootHomeTabPeriodContentUiState.Event {
        override fun onClickNextMonth() {
            val period = viewModelStateFlow.value.displayPeriod

            fetch(
                period = period.copy(
                    sinceDate = period.sinceDate.addMonth(1),
                )
            )
        }

        override fun onClickPreviousMonth() {
            val period = viewModelStateFlow.value.displayPeriod

            fetch(
                period = period.copy(
                    sinceDate = period.sinceDate.addMonth(-1),
                )
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
                )
            )
        }

        override fun onViewInitialized() {
            fetch(viewModelStateFlow.value.displayPeriod)
        }

        override fun onClickRetry() {
            fetch(viewModelStateFlow.value.displayPeriod)
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
                        categoryType = when (viewModelState.contentType) {
                            is ViewModelState.ContentType.All -> "すべて"
                            is ViewModelState.ContentType.Category -> viewModelState.contentType.name
                        },
                        categoryTypes = createCategoryTypes(categories = categories).toImmutableList(),
                        between = "${displayPeriods.first().year}/${displayPeriods.first().month} - ${displayPeriods.last().year}/${displayPeriods.last().month}",
                        rangeText = "${viewModelState.displayPeriod.monthCount}ヶ月",
                        graphContent = createTotalUiState(
                            responses = responses,
                            categories = categories,
                        ) ?: return@screenState RootHomeTabPeriodContentUiState.LoadingState.Error,
                    )
                }

                uiStateFlow.value = RootHomeTabPeriodContentUiState(
                    loadingState = loadingState,
                    event = event,
                )
            }
        }
    }.asStateFlow()

    private fun createCategoryTypes(
        categories: List<RootHomeTabScreenAnalyticsByDateQuery.Category>,
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
                )
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
                        },
                    )
                }
            )
        }
    }

    private fun createTotalUiState(
        responses: List<Pair<ViewModelState.YearMonth, ApolloResponse<RootHomeTabScreenAnalyticsByDateQuery.Data>>>,
        categories: List<RootHomeTabScreenAnalyticsByDateQuery.Category>
    ): RootHomeTabPeriodContentUiState.GraphContent.Total? {
        return RootHomeTabPeriodContentUiState.GraphContent.Total(
            barGraph = BarGraphUiState(
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
                                    ?: return null

                            byCategory.map {
                                val amount = it.totalAmount
                                    ?: return null
                                BarGraphUiState.Item(
                                    color = reservedColorModel.getColor(it.category.id.id.toString()),
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
                    color = reservedColorModel.getColor(it.id.id.toString()),
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
                    },
                )
            }.toImmutableList(),
            monthTotalItems = responses.map { (yearMonth, response) ->
                RootHomeTabPeriodContentUiState.MonthTotalItem(
                    amount = Formatter.formatMoney(
                        response.data?.user?.moneyUsageAnalytics?.totalAmount
                            ?: return null
                    ) + "円",
                    title = "${yearMonth.year}/${yearMonth.month.toString().padStart(2, '0')}",
                )
            }.toImmutableList(),
        )
    }

    private fun fetch(period: ViewModelState.Period) {
        coroutineScope.launch {
            (0 until period.monthCount)
                .map { index ->
                    val start = period.sinceDate.addMonth(index)

                    start to start.addMonth(1)
                }
                .filter { (startYearMonth, _) ->
                    viewModelStateFlow.value.responseMap.contains(startYearMonth).not()
                }
                .map { (startYearMonth, endYearMonth) ->
                    async {
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
                }.map { it.await() }
            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = period,
                )
            }
        }
    }

    private data class ViewModelState(
        val contentType: ContentType = ContentType.All,
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

        sealed interface ContentType {
            object All : ContentType
            data class Category(
                val categoryId: MoneyUsageCategoryId,
                val name: String,
            ) : ContentType
        }
    }
}
