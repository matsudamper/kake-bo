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
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.layout.graph.PolygonalLineGraphItemUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodCategoryContentUiState
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodContentUiState
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenAnalyticsByCategoryQuery

public class RootHomeTabPeriodCategoryContentViewModel(
    private val categoryId: MoneyUsageCategoryId,
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(ViewModelState(categoryId = categoryId))
    private val reservedColorModel = ReservedColorModel()

    public val uiStateFlow: StateFlow<RootHomeTabPeriodCategoryContentUiState> = MutableStateFlow<RootHomeTabPeriodCategoryContentUiState>(
        RootHomeTabPeriodCategoryContentUiState(
            loadingState = RootHomeTabPeriodCategoryContentUiState.LoadingState.Loading,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                    viewModelState.displayPeriod.sinceDate.addMonth(index)
                }
                uiStateFlow.update {
                    createCategoryUiState(
                        categoryId = viewModelState.categoryId,
                        displayPeriods = displayPeriods,
                        categoryResponseMap = viewModelState.categoryResponseMap,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createCategoryUiState(
        categoryId: MoneyUsageCategoryId,
        displayPeriods: List<ViewModelState.YearMonth>,
        categoryResponseMap: Map<ViewModelState.YearMonthCategory, ApolloResponse<RootHomeTabScreenAnalyticsByCategoryQuery.Data>?>,
    ): RootHomeTabPeriodCategoryContentUiState {
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

                    RootHomeTabPeriodContentUiState.MonthTotalItem(
                        title = "${yearMonth.year}/${yearMonth.month.toString().padStart(2, '0')}",
                        amount = Formatter.formatMoney(result.totalAmount ?: return@loadingState RootHomeTabPeriodCategoryContentUiState.LoadingState.Error) + "円",
                    )
                }.toImmutableList(),
            )
        }

        return RootHomeTabPeriodCategoryContentUiState(
            loadingState = loadingState,
        )
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
