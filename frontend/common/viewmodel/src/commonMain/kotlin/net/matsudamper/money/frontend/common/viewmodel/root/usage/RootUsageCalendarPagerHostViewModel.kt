package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
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
            yearMonth = getDisplayYearMonth(initial),
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
            },
        )
    }

    private val betweenPageCount = 100
    public val uiState: StateFlow<RootUsageCalendarPagerHostScreenUiState> = MutableStateFlow(
        RootUsageCalendarPagerHostScreenUiState(
            pages = buildList {
                val current = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val yearMonth = initial.yearMonth
                val initialDate = if (yearMonth != null) {
                    LocalDate(
                        year = yearMonth.year,
                        month = yearMonth.month,
                        day = 1,
                    )
                } else {
                    LocalDate(
                        year = current.year,
                        month = current.month,
                        day = 1,
                    )
                }
                for (index in -betweenPageCount..betweenPageCount) {
                    val date = initialDate.plus(
                        value = index,
                        unit = DateTimeUnit.MONTH,
                    )
                    add(
                        RootUsageCalendarPagerHostScreenUiState.Page(
                            navigation = ScreenStructure.Root.Usage.Calendar(
                                yearMonth = ScreenStructure.Root.Usage.Calendar.YearMonth(
                                    year = date.year,
                                    month = date.month.number,
                                ),
                            ),
                        ),
                    )
                }
            }.toImmutableList(),
            currentPage = betweenPageCount,
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
                val yearMonth = calendarViewModelState.yearMonth
                rootUsageHostViewModel.updateHeaderTitle(
                    "${yearMonth.year}/${yearMonth.month.number}",
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
                        currentPage = uiState.pages.indexOfFirst { page ->
                            page.navigation.yearMonth == viewModelState.yearMonth
                        }.takeIf { it >= 0 } ?: uiState.currentPage,
                    )
                }
            }
        }
    }.asStateFlow()

    public fun updateStructure(current: ScreenStructure.Root.Usage.Calendar) {
        viewModelStateFlow.update {
            it.copy(
                yearMonth = getDisplayYearMonth(current),
            )
        }
    }

    private fun prevMonth() {
        viewModelStateFlow.update {
            val month = it.yearMonth.minus(1, DateTimeUnit.MONTH)
            rootUsageHostViewModel.calendarPagingModel.changeMonth(month.firstDay)
            it.copy(
                yearMonth = month,
            )
        }
        viewModelScope.launch {
            viewModelEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Usage.Calendar(
                        yearMonth = ScreenStructure.Root.Usage.Calendar.YearMonth(
                            year = viewModelStateFlow.value.yearMonth.year,
                            month = viewModelStateFlow.value.yearMonth.month.number,
                        ),
                    ),
                )
            }
            rootUsageHostViewModel.calendarPagingModel.fetch()
        }
    }

    private fun nextMonth() {
        viewModelStateFlow.update {
            val month = it.yearMonth.plus(1, DateTimeUnit.MONTH)
            rootUsageHostViewModel.calendarPagingModel.changeMonth(month.firstDay)
            it.copy(
                yearMonth = month,
            )
        }
        viewModelScope.launch {
            viewModelEventSender.send {
                it.navigate(
                    ScreenStructure.Root.Usage.Calendar(
                        yearMonth = ScreenStructure.Root.Usage.Calendar.YearMonth(
                            year = viewModelStateFlow.value.yearMonth.year,
                            month = viewModelStateFlow.value.yearMonth.month.number,
                        ),
                    ),
                )
            }
            rootUsageHostViewModel.calendarPagingModel.fetch()
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
        val yearMonth: YearMonth,
    )
}
