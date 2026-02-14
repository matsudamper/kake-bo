package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlin.coroutines.coroutineContext
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.isFromCache
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodSubCategoryContentUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodSubCategoryContentViewModel.ViewModelState.Period
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabPeriodSubCategoryContentViewModel.ViewModelState.YearMonth
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsBySubCategoryQuery

public class RootHomeTabPeriodSubCategoryContentViewModel(
    structure: RootHomeScreenStructure.PeriodSubCategory,
    scopedObjectFeature: ScopedObjectFeature,
    private val api: RootHomeAnalyticsSubCategoryApi,
    loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val reservedColorModel = ReservedColorModel()
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            subCategoryId = structure.subCategoryId,
            displayPeriod = run {
                val since = structure.since ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                Period(
                    sinceDate = YearMonth(since.year, since.monthNumber)
                        .addMonth(if (structure.since == null) -5 else 0),
                    monthCount = structure.period,
                )
            },
        ),
    )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private fun onClickNextMonth() {
        viewModelStateFlow.update {
            val period = it.displayPeriod
            it.copy(
                displayPeriod = period.copy(
                    sinceDate = period.sinceDate.addMonth(1),
                ),
            )
        }
        emitNavigationUpdate()
    }

    private fun onClickPreviousMonth() {
        viewModelStateFlow.update {
            val period = it.displayPeriod
            it.copy(
                displayPeriod = period.copy(
                    sinceDate = period.sinceDate.addMonth(-1),
                ),
            )
        }
        emitNavigationUpdate()
    }

    private fun onClickRange(range: Int) {
        val currentEndYearMonth = viewModelStateFlow.value.displayPeriod.let { period ->
            period.sinceDate.addMonth(period.monthCount)
        }
        val newSinceDate = currentEndYearMonth.addMonth(-range)

        viewModelStateFlow.update {
            it.copy(
                displayPeriod = Period(
                    sinceDate = newSinceDate,
                    monthCount = range,
                ),
            )
        }
        emitNavigationUpdate()
    }

    private fun emitNavigationUpdate() {
        viewModelScope.launch {
            val state = viewModelStateFlow.value
            eventSender.send {
                it.navigate(
                    RootHomeScreenStructure.PeriodSubCategory(
                        subCategoryId = state.subCategoryId,
                        since = LocalDate(
                            year = state.displayPeriod.sinceDate.year,
                            monthNumber = state.displayPeriod.sinceDate.month,
                            dayOfMonth = 1,
                        ),
                        period = state.displayPeriod.monthCount,
                    ),
                )
            }
        }
    }

    public fun updateStructure(current: RootHomeScreenStructure.PeriodSubCategory) {
        val since = current.since
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                subCategoryId = current.subCategoryId,
                displayPeriod = viewModelState.displayPeriod.copy(
                    sinceDate = if (since != null) {
                        YearMonth(
                            year = since.year,
                            month = since.monthNumber,
                        )
                    } else {
                        viewModelState.displayPeriod.sinceDate
                    },
                    monthCount = current.period,
                ),
            )
        }
    }

    public val uiStateFlow: StateFlow<RootHomeTabPeriodSubCategoryContentUiState> = MutableStateFlow(
        RootHomeTabPeriodSubCategoryContentUiState(
            loadingState = RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            periodUiState = createPeriodUiState(viewModelStateFlow.value.displayPeriod),
            event = object : RootHomeTabPeriodSubCategoryContentUiState.Event {
                override suspend fun onViewInitialized() {
                    viewModelScope.launch {
                        loginCheckUseCase.check()
                    }
                    viewModelScope.launch(coroutineContext) {
                        viewModelStateFlow.map { it.displayPeriod }
                            .stateIn(this)
                            .collectLatest { displayPeriod ->
                                for (i in 0 until displayPeriod.monthCount) {
                                    launch {
                                        collectSubCategory(
                                            month = displayPeriod.sinceDate.addMonth(i),
                                        )
                                    }
                                }
                            }
                    }
                }

                override fun refresh() {
                    // TODO
                }

                override fun onClickNextMonth() {
                    this@RootHomeTabPeriodSubCategoryContentViewModel.onClickNextMonth()
                }

                override fun onClickPreviousMonth() {
                    this@RootHomeTabPeriodSubCategoryContentViewModel.onClickPreviousMonth()
                }

                override fun onClickRange(range: Int) {
                    this@RootHomeTabPeriodSubCategoryContentViewModel.onClickRange(range)
                }
            },
        ),
    ).also { mutableUiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val loadingState: RootHomeTabPeriodSubCategoryContentUiState.LoadingState = run loadingState@{
                    val graphItems: MutableList<BarGraphUiState.PeriodData> = mutableListOf()
                    val monthTotalItems: MutableList<RootHomeTabPeriodSubCategoryContentUiState.MonthTotalItem> = mutableListOf()
                    var subCategoryName = ""
                    for (i in 0 until viewModelState.displayPeriod.monthCount) {
                        val yearMonth = viewModelState.displayPeriod.sinceDate.addMonth(i)

                        val result = viewModelState.resultMap[yearMonth]
                        if (result == null) {
                            return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading
                        }
                        val bySubCategory = result.getOrNull()?.data?.user?.moneyUsageAnalyticsBySubCategory
                        if (bySubCategory == null) {
                            return@loadingState RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Error
                        }

                        subCategoryName = bySubCategory.subCategory.name
                        val amount = bySubCategory.totalAmount ?: 0
                        graphItems.add(
                            BarGraphUiState.PeriodData(
                                year = yearMonth.year,
                                month = yearMonth.month,
                                items = immutableListOf(
                                    BarGraphUiState.Item(
                                        color = reservedColorModel.getColor(""),
                                        title = bySubCategory.subCategory.name,
                                        value = amount,
                                    ),
                                ),
                                total = amount,
                                event = object : BarGraphUiState.PeriodDataEvent {
                                    override fun onClick() {
                                        viewModelScope.launch {
                                            eventSender.send {
                                                it.navigate(
                                                    RootHomeScreenStructure.MonthlySubCategory(
                                                        subCategoryId = viewModelState.subCategoryId,
                                                        year = yearMonth.year,
                                                        month = yearMonth.month,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                },
                            ),
                        )
                        monthTotalItems.add(
                            RootHomeTabPeriodSubCategoryContentUiState.MonthTotalItem(
                                title = "${yearMonth.year}/${yearMonth.month}",
                                amount = "${amount}円",
                                event = object : RootHomeTabPeriodSubCategoryContentUiState.MonthTotalItem.Event {
                                    override fun onClick() {
                                    }
                                },
                            ),
                        )
                    }
                    RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loaded(
                        graphItems = BarGraphUiState(graphItems.toImmutableList()),
                        monthTotalItems = monthTotalItems.toImmutableList(),
                        subCategoryName = subCategoryName,
                    )
                }

                mutableUiStateFlow.update {
                    it.copy(
                        loadingState = loadingState,
                        periodUiState = createPeriodUiState(viewModelState.displayPeriod),
                    )
                }
            }
        }
    }.asStateFlow()

    private suspend fun collectSubCategory(
        month: YearMonth,
    ) {
        val start = month
        val end = start.addMonth(1)
        val flow = api.watch(
            subCategory = viewModelStateFlow.value.subCategoryId,
            startYear = start.year,
            startMonth = start.month,
            endYear = end.year,
            endMonth = end.month,
            useCache = true,
        )
        flow
            .catch { e ->
                e.printStackTrace()
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        resultMap = viewModelState.resultMap.plus(start to Result.failure(e)),
                    )
                }
            }
            .collectLatest { response ->
                if (response.isFromCache && response.data == null) {
                    return@collectLatest
                }
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        resultMap = viewModelState.resultMap.plus(start to Result.success(response)),
                    )
                }
            }
    }

    private data class ViewModelState(
        val subCategoryId: MoneyUsageSubCategoryId,
        val displayPeriod: Period,
        val resultMap: Map<YearMonth, Result<ApolloResponse<RootHomeTabScreenAnalyticsBySubCategoryQuery.Data>>> = mapOf(),
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

    private companion object {
        fun createPeriodUiState(displayPeriod: Period): RootHomeTabPeriodSubCategoryContentUiState.PeriodUiState {
            val first = displayPeriod.sinceDate
            val last = displayPeriod.sinceDate.addMonth(displayPeriod.monthCount - 1)
            return RootHomeTabPeriodSubCategoryContentUiState.PeriodUiState(
                between = "${first.year}/${first.month} - ${last.year}/${last.month}",
                rangeText = "${displayPeriod.monthCount}ヶ月",
            )
        }
    }
}
