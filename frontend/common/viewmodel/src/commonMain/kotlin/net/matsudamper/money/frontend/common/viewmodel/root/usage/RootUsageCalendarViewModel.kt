package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreenUiState
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.UsageCalendarScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class RootUsageCalendarViewModel(
    private val coroutineScope: CoroutineScope,
    private val rootUsageHostViewModel: RootUsageHostViewModel,
) {
    internal val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val calendarHeaderEvent = object : RootUsageHostScreenUiState.HeaderCalendarEvent {
        override fun onClickPrevMonth() {
            prevMonth()
        }

        override fun onClickNextMonth() {
            nextMonth()
        }
    }

    public val uiStateFlow: StateFlow<RootUsageCalendarScreenUiState> = MutableStateFlow(
        RootUsageCalendarScreenUiState(
            loadingState = RootUsageCalendarScreenUiState.LoadingState.Loading,
            hostScreenUiState = rootUsageHostViewModel.uiStateFlow.value,
            event = object : RootUsageCalendarScreenUiState.Event {
                override fun onViewInitialized() {
                    rootUsageHostViewModel.pagingModel.fetch()
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            rootUsageHostViewModel.uiStateFlow.collectLatest {
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        hostScreenUiState = it,
                    )
                }
            }
        }
        coroutineScope.launch {
            viewModelStateFlow.collectLatest {
                val calendarViewModelState = it
                rootUsageHostViewModel.updateHeader(
                    RootUsageHostScreenUiState.Header.Calendar(
                        title = "${calendarViewModelState.displayMonth.year}/${calendarViewModelState.displayMonth.monthNumber}",
                        event = calendarHeaderEvent,
                    ),
                )
            }
        }
        coroutineScope.launch {
            viewModelStateFlow
                .collectLatest { viewModelState ->
                    val nodes = viewModelState.results.mapNotNull { state ->
                        state.getSuccessOrNull()?.value
                    }.flatMap {
                        it.data?.user?.moneyUsages?.nodes.orEmpty()
                    }

                    val cells: ImmutableList<RootUsageCalendarScreenUiState.CalendarCell> = run cells@{
                        val dayGroup = nodes.groupBy { it.date.date.dayOfMonth }

                        val firstDay = viewModelState.displayMonth
                            .let { LocalDate(it.year, it.monthNumber, 1) }

                        val daysOfMonth = run daysOfMonth@{
                            buildList<LocalDate> {
                                add(firstDay)
                                while (last().month == firstDay.month) {
                                    add(last().plus(DateTimeUnit.DayBased(1)))
                                }
                                removeLast()
                            }
                        }
                        val padding = when (firstDay.dayOfWeek) {
                            DayOfWeek.SUNDAY -> 0
                            DayOfWeek.MONDAY -> 1
                            DayOfWeek.TUESDAY -> 2
                            DayOfWeek.WEDNESDAY -> 3
                            DayOfWeek.THURSDAY -> 4
                            DayOfWeek.FRIDAY -> 5
                            DayOfWeek.SATURDAY -> 6
                        }
                        buildList {
                            addAll(
                                listOf(
                                    DayOfWeek.SUNDAY,
                                    DayOfWeek.MONDAY,
                                    DayOfWeek.TUESDAY,
                                    DayOfWeek.WEDNESDAY,
                                    DayOfWeek.THURSDAY,
                                    DayOfWeek.FRIDAY,
                                    DayOfWeek.SATURDAY,
                                ).map {
                                    RootUsageCalendarScreenUiState.CalendarCell.DayOfWeek(
                                        text = when (it) {
                                            DayOfWeek.SUNDAY -> "日"
                                            DayOfWeek.MONDAY -> "月"
                                            DayOfWeek.TUESDAY -> "火"
                                            DayOfWeek.WEDNESDAY -> "水"
                                            DayOfWeek.THURSDAY -> "木"
                                            DayOfWeek.FRIDAY -> "金"
                                            DayOfWeek.SATURDAY -> "土"
                                        },
                                        dayOfWeek = it,
                                    )
                                },
                            )
                            addAll(
                                (0 until padding).map {
                                    RootUsageCalendarScreenUiState.CalendarCell.Empty
                                },
                            )
                            addAll(
                                daysOfMonth.map { localDate ->
                                    val days = dayGroup[localDate.dayOfMonth].orEmpty()

                                    RootUsageCalendarScreenUiState.CalendarCell.Day(
                                        text = "${localDate.dayOfMonth}日",
                                        isToday = localDate == viewModelState.today,
                                        items = days.map { day ->
                                            RootUsageCalendarScreenUiState.CalendarDayItem(
                                                title = day.title,
                                                event = object : RootUsageCalendarScreenUiState.CalendarDayEvent {
                                                    override fun onClick() {
                                                        coroutineScope.launch {
                                                            viewModelEventSender.send {
                                                                it.navigate(ScreenStructure.MoneyUsage(day.id))
                                                            }
                                                        }
                                                    }
                                                },
                                            )
                                        }.toImmutableList(),
                                    )
                                },
                            )
                        }
                    }.toImmutableList()

                    uiStateFlow.update { uiState ->
                        uiState.copy(
                            loadingState = RootUsageCalendarScreenUiState.LoadingState.Loaded(
                                calendarCells = cells,
                                event = object : RootUsageCalendarScreenUiState.LoadedEvent {
                                    override fun loadMore() {
                                        rootUsageHostViewModel.pagingModel.fetch()
                                    }
                                },
                            ),
                        )
                    }
                }
        }
    }.asStateFlow()

    init {
        rootUsageHostViewModel.pagingModel.changeMonth(viewModelStateFlow.value.displayMonth)
        coroutineScope.launch {
            rootUsageHostViewModel.pagingModel.flow.collectLatest { responseStates ->
                viewModelStateFlow.update {
                    it.copy(
                        results = responseStates,
                    )
                }
            }
        }
        coroutineScope.launch {
            rootUsageHostViewModel.pagingModel.flow.map { it.lastOrNull()?.getSuccessOrNull() }
                .collectLatest {
                    if (it?.value?.data?.user?.moneyUsages?.hasMore == true) {
                        rootUsageHostViewModel.pagingModel.fetch()
                    }
                }
        }
    }

    internal fun prevMonth() {
        viewModelStateFlow.update {
            val month = it.displayMonth.minus(1, DateTimeUnit.MONTH)
            rootUsageHostViewModel.pagingModel.changeMonth(month)
            it.copy(
                displayMonth = month,
            )
        }
        rootUsageHostViewModel.pagingModel.fetch()
    }

    internal fun nextMonth() {
        viewModelStateFlow.update {
            val month = it.displayMonth.plus(1, DateTimeUnit.MONTH)
            rootUsageHostViewModel.pagingModel.changeMonth(month)
            it.copy(
                displayMonth = month,
            )
        }
        rootUsageHostViewModel.pagingModel.fetch()
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    public data class ViewModelState(
        val results: List<ApolloResponseState<ApolloResponse<UsageCalendarScreenPagingQuery.Data>>> = listOf(),
        val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        val displayMonth: LocalDate = run {
            val currentLocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            LocalDate(
                year = currentLocalDateTime.year,
                monthNumber = currentLocalDateTime.monthNumber,
                dayOfMonth = 1,
            )
        },
    )
}
