package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.plusMonth
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarPagerHostScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel

/**
 * 表示中の年月はこの ViewModel が保持する。
 * ナビゲーションの更新を待ってから表示を切り替えると、スワイプやヘッダー操作の結果が反映されないケースがあるため、
 * 操作時は先に状態を更新し、URL / バックスタックの同期は後追いで行う。
 */
public class RootUsageCalendarPagerHostViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    initial: ScreenStructure.Root.Usage.Calendar,
    private val rootUsageHostViewModel: RootUsageHostViewModel,
    private val navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val pageYearMonths: List<YearMonth> = (-PAGE_COUNT_FROM_TODAY..PAGE_COUNT_FROM_TODAY).map { index ->
        today.yearMonth.plus(value = index, unit = DateTimeUnit.MONTH)
    }
    private val pages: ImmutableList<RootUsageCalendarPagerHostScreenUiState.Page> = pageYearMonths.map { yearMonth ->
        RootUsageCalendarPagerHostScreenUiState.Page(
            navigation = yearMonth.toScreenStructure(),
        )
    }.toImmutableList()

    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            displayYearMonth = initial.toDisplayYearMonth(),
        ),
    )

    private val headerCalendarEvent = object : RootUsageHostScreenUiState.HeaderCalendarEvent {
        override fun onClickPrevMonth() {
            moveTo(viewModelStateFlow.value.displayYearMonth.minusMonth())
        }

        override fun onClickNextMonth() {
            moveTo(viewModelStateFlow.value.displayYearMonth.plusMonth())
        }

        override fun onClickYearMonth(year: Int, month: Int) {
            moveTo(YearMonth(year = year, month = month))
        }
    }

    private val event = object : RootUsageCalendarPagerHostScreenUiState.Event {
        override fun onPageChanged(page: RootUsageCalendarPagerHostScreenUiState.Page) {
            val yearMonth = page.navigation.yearMonth ?: return
            moveTo(YearMonth(year = yearMonth.year, month = yearMonth.month))
        }
    }

    public val uiState: StateFlow<RootUsageCalendarPagerHostScreenUiState> = MutableStateFlow(
        createUiState(
            viewModelState = viewModelStateFlow.value,
            hostScreenUiState = rootUsageHostViewModel.uiStateFlow.value,
        ),
    ).also { mutableUiStateFlow ->
        viewModelScope.launch {
            combine(viewModelStateFlow, rootUsageHostViewModel.uiStateFlow) { viewModelState, hostScreenUiState ->
                createUiState(
                    viewModelState = viewModelState,
                    hostScreenUiState = hostScreenUiState,
                )
            }.collect { uiState ->
                mutableUiStateFlow.value = uiState
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                rootUsageHostViewModel.updateCalendarYearMonth(
                    year = viewModelState.displayYearMonth.year,
                    month = viewModelState.displayYearMonth.month.number,
                )
            }
        }
    }.asStateFlow()

    public fun updateStructure(current: ScreenStructure.Root.Usage.Calendar) {
        val yearMonth = current.toDisplayYearMonth()
        if (yearMonth !in pageYearMonths) return
        viewModelStateFlow.update { it.copy(displayYearMonth = yearMonth) }
    }

    private fun moveTo(yearMonth: YearMonth) {
        if (yearMonth !in pageYearMonths) return
        if (viewModelStateFlow.value.displayYearMonth == yearMonth) return
        viewModelStateFlow.update { it.copy(displayYearMonth = yearMonth) }
        navController.navigateReplace(yearMonth.toScreenStructure())
    }

    private fun createUiState(
        viewModelState: ViewModelState,
        hostScreenUiState: RootUsageHostScreenUiState,
    ): RootUsageCalendarPagerHostScreenUiState {
        val displayYearMonth = viewModelState.displayYearMonth
        return RootUsageCalendarPagerHostScreenUiState(
            pages = pages,
            currentPage = pageYearMonths.indexOf(displayYearMonth),
            hostScreenUiState = hostScreenUiState.copy(
                type = RootUsageHostScreenUiState.Type.Calendar,
                header = RootUsageHostScreenUiState.Header.Calendar(
                    title = "${displayYearMonth.year}/${displayYearMonth.month.number}",
                    year = displayYearMonth.year,
                    month = displayYearMonth.month.number,
                    currentYear = today.year,
                    currentMonth = today.month.number,
                    event = headerCalendarEvent,
                ),
            ),
            event = event,
        )
    }

    private fun ScreenStructure.Root.Usage.Calendar.toDisplayYearMonth(): YearMonth {
        val yearMonth = yearMonth ?: return today.yearMonth
        return YearMonth(year = yearMonth.year, month = yearMonth.month)
    }

    private fun YearMonth.toScreenStructure(): ScreenStructure.Root.Usage.Calendar {
        return ScreenStructure.Root.Usage.Calendar(
            yearMonth = ScreenStructure.Root.Usage.Calendar.YearMonth(
                year = year,
                month = month.number,
            ),
        )
    }

    private data class ViewModelState(
        val displayYearMonth: YearMonth,
    )

    private companion object {
        private const val PAGE_COUNT_FROM_TODAY = 12 * 100
    }
}
