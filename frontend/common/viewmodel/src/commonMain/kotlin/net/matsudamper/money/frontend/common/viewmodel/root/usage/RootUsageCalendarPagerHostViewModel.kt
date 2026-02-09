package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarPagerHostScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class RootUsageCalendarPagerHostViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    initial: ScreenStructure.Root.Usage.Calendar,
    private val rootUsageHostViewModel: RootUsageHostViewModel,
    private val navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            currentYearMonth = getDisplayYearMonth(initial),
        ),
    )

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    init {
        rootUsageHostViewModel.updateEventListener(
            object : RootUsageHostScreenUiState.HeaderCalendarEvent {
                override fun onClickPrevMonth() {
                    prevMonth()
                }

                override fun onClickNextMonth() {
                    nextMonth()
                }

                override fun onClickYearMonth(year: Int, month: Int) {
                    navigateToYearMonth(year = year, month = month)
                }
            },
        )
    }

    public val uiState: StateFlow<RootUsageCalendarPagerHostScreenUiState> = MutableStateFlow(
        RootUsageCalendarPagerHostScreenUiState(
            pages = immutableListOf(),
            currentPage = null,
            hostScreenUiState = rootUsageHostViewModel.uiStateFlow.value,
            event = object : RootUsageCalendarPagerHostScreenUiState.Event {
                override fun onPageChanged(page: RootUsageCalendarPagerHostScreenUiState.Page) {
                    navController.navigate(
                        ScreenStructure.Root.Usage.Calendar(
                            yearMonth = page.navigation.yearMonth,
                        ),
                    )
                }
            },
        ),
    ).also { mutableUiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { calendarViewModelState ->
                val yearMonth = calendarViewModelState.currentYearMonth
                rootUsageHostViewModel.updateHeaderTitle(
                    "${yearMonth.year}/${yearMonth.month.number}",
                )
                rootUsageHostViewModel.updateCalendarYearMonth(
                    year = yearMonth.year,
                    month = yearMonth.month.number,
                )
            }
        }
        viewModelScope.launch {
            rootUsageHostViewModel.uiStateFlow
                .collectLatest { hostUiState ->
                    mutableUiStateFlow.update { uiState ->
                        uiState.copy(
                            hostScreenUiState = hostUiState,
                        )
                    }
                }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                mutableUiStateFlow.update { uiState ->
                    uiState.copy(
                        currentPage = viewModelState.pages.indexOf(viewModelState.currentYearMonth)
                            .takeIf { it >= 0 }
                            ?: TODO("無限スクロールをどうするか考える"),
                        pages = viewModelState.pages.map { page ->
                            RootUsageCalendarPagerHostScreenUiState.Page(
                                navigation = ScreenStructure.Root.Usage.Calendar(
                                    yearMonth = ScreenStructure.Root.Usage.Calendar.YearMonth(
                                        year = page.year,
                                        month = page.month.number,
                                    ),
                                ),
                            )
                        }.toImmutableList(),
                    )
                }
            }
        }
    }.asStateFlow()

    public fun updateStructure(current: ScreenStructure.Root.Usage.Calendar) {
        viewModelStateFlow.update {
            it.copy(
                currentYearMonth = getDisplayYearMonth(current),
            )
        }
    }

    private fun prevMonth() {
        val prev = viewModelStateFlow.value.currentYearMonth.minusMonth()
        viewModelScope.launch {
            viewModelEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Usage.Calendar(
                        yearMonth = ScreenStructure.Root.Usage.Calendar.YearMonth(
                            year = prev.year,
                            month = prev.month.number,
                        ),
                    ),
                )
            }
        }
    }

    private fun nextMonth() {
        val next = viewModelStateFlow.value.currentYearMonth.plusMonth()
        viewModelScope.launch {
            viewModelEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Usage.Calendar(
                        yearMonth = ScreenStructure.Root.Usage.Calendar.YearMonth(
                            year = next.year,
                            month = next.month.number,
                        ),
                    ),
                )
            }
        }
    }

    private fun navigateToYearMonth(year: Int, month: Int) {
        viewModelScope.launch {
            viewModelEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Usage.Calendar(
                        yearMonth = ScreenStructure.Root.Usage.Calendar.YearMonth(
                            year = year,
                            month = month,
                        ),
                    ),
                )
            }
        }
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    private fun getDisplayYearMonth(calendar: ScreenStructure.Root.Usage.Calendar): YearMonth {
        val now = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yearMonth = calendar.yearMonth ?: return now.yearMonth
        return YearMonth(
            year = yearMonth.year,
            month = yearMonth.month,
        )
    }

    public data class ViewModelState(
        val currentYearMonth: YearMonth,
        val pages: List<YearMonth> = buildList {
            val betweenPageCount = 100

            val current = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val currentYearMonth = current.yearMonth

            for (index in -betweenPageCount..betweenPageCount) {
                val date = currentYearMonth.plus(
                    value = index,
                    unit = DateTimeUnit.MONTH,
                )
                add(date)
            }
        },
    )
}
