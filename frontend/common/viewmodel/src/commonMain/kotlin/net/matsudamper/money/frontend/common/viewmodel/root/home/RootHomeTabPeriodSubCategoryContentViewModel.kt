package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.bar.BarGraphUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodAndCategoryUiState.LoadingState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodSubCategoryContentUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.PlatformType
import net.matsudamper.money.frontend.common.viewmodel.PlatformTypeProvider
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl
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

    public val uiStateFlow: StateFlow<RootHomeTabPeriodSubCategoryContentUiState> = MutableStateFlow(
        RootHomeTabPeriodSubCategoryContentUiState(
            loadingState = RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading,
            rootScaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
                override fun onClickHome() {
                    if (PlatformTypeProvider.type == PlatformType.JS) {
                        super.onClickHome()
                    } else {
                        // TODO scroll to top
                    }
                }
            },
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
                                    val start = displayPeriod.sinceDate.addMonth(i)
                                    val end = displayPeriod.sinceDate.addMonth(i + 1)
                                    launch {
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
                                                viewModelStateFlow.update { viewModelState ->
                                                    viewModelState.copy(
                                                        resultMap = viewModelState.resultMap.plus(start to Result.success(response)),
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                    }
                }

                override fun refresh() {
                    // TODO
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
                                                    )
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
                    )
                }
            }
        }
    }.asStateFlow()

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
}
