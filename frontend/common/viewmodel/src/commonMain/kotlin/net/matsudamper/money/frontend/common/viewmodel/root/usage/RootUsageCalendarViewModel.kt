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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageCalendarScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryFilter

public class RootUsageCalendarViewModel(
    private val coroutineScope: CoroutineScope,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    internal val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val paging = ApolloPagingResponseCollector.create<UsageCalendarScreenPagingQuery.Data>(
        apolloClient = apolloClient,
        coroutineScope = coroutineScope,
    )

    public val uiStateFlow: StateFlow<RootUsageCalendarScreenUiState> = MutableStateFlow(
        RootUsageCalendarScreenUiState(
            loadingState = RootUsageCalendarScreenUiState.LoadingState.Loading,
            event = object : RootUsageCalendarScreenUiState.Event {
                override fun onViewInitialized() {
                    fetch()
                }

                override fun onClickAdd() {
                    coroutineScope.launch {
                        viewModelEventSender.send {
                            it.navigate(ScreenStructure.AddMoneyUsage())
                        }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
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

                        val daysOfMonth = run daysOfMonth@{
                            val firstDay = dayGroup.toList()
                                .minByOrNull { it.first }
                                ?.second?.firstOrNull()?.date?.date
                                ?.let { LocalDate(it.year, it.monthNumber, 1) } ?: return@cells null

                            buildList<LocalDate> {
                                add(firstDay)
                                while (last().month == firstDay.month) {
                                    add(last().plus(DateTimeUnit.DayBased(1)))
                                }
                                removeLast()
                            }
                        }

                        daysOfMonth.map { localDate ->
                            val day = dayGroup[localDate.dayOfMonth].orEmpty()

                            RootUsageCalendarScreenUiState.CalendarCell.Day(
                                text = "${localDate.dayOfMonth}日",
                                items = day.map { node ->
                                    RootUsageCalendarScreenUiState.CalendarDayItem(
                                        title = node.title,
                                    )
                                }.toImmutableList(),
                            )
                        }
                    }.orEmpty().toImmutableList()

                    uiStateFlow.update { uiState ->
                        uiState.copy(
                            loadingState = RootUsageCalendarScreenUiState.LoadingState.Loaded(
                                calendarCells = cells,
                                event = object : RootUsageCalendarScreenUiState.LoadedEvent {
                                    override fun loadMore() {
                                        fetch()
                                    }
                                },
                            ),
                        )
                    }
                }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            paging.flow.collectLatest { responseStates ->
                viewModelStateFlow.update {
                    it.copy(
                        results = responseStates,
                    )
                }
            }
        }
        coroutineScope.launch {
            paging.flow.map { it.lastOrNull()?.getSuccessOrNull() }
                .collectLatest {
                    if (it?.value?.data?.user?.moneyUsages?.hasMore == true) {
                        fetch()
                    }
                }
        }
    }

    private fun fetch() {
        paging.add { collectors ->
            val cursor: String?
            when (val lastState = collectors.lastOrNull()?.flow?.value) {
                is ApolloResponseState.Loading -> return@add null
                is ApolloResponseState.Failure -> {
                    paging.lastRetry()
                    return@add null
                }

                null -> {
                    cursor = null
                }

                is ApolloResponseState.Success -> {
                    val result = lastState.value.data?.user?.moneyUsages
                    if (result == null) {
                        paging.lastRetry()
                        return@add null
                    }
                    if (result.hasMore.not()) return@add null

                    cursor = result.cursor
                }
            }
            UsageCalendarScreenPagingQuery(
                query = MoneyUsagesQuery(
                    cursor = Optional.present(cursor),
                    filter = Optional.present(
                        MoneyUsagesQueryFilter(
                            sinceDateTime = Optional.present(
                                LocalDateTime(
                                    LocalDate(
                                        year = viewModelStateFlow.value.displayMonth.year,
                                        month = viewModelStateFlow.value.displayMonth.month,
                                        dayOfMonth = 1,
                                    ),
                                    LocalTime(0, 0),
                                ),
                            ),
                            untilDateTime = Optional.present(
                                LocalDateTime(
                                    LocalDate(
                                        year = viewModelStateFlow.value.displayMonth.year,
                                        monthNumber = viewModelStateFlow.value.displayMonth.monthNumber + 1,
                                        dayOfMonth = 1,
                                    ).minus(1, DateTimeUnit.DAY),
                                    LocalTime(0, 0),
                                ),
                            ),
                        ),
                    ),
                    isAsc = false,
                    size = 10,
                ),
            )
        }
    }

    internal fun prevMonth() {
        viewModelStateFlow.update {
            it.copy(
                displayMonth = it.displayMonth.minus(1, DateTimeUnit.MONTH),
            )
        }
        paging.clear()
        fetch()
    }

    internal fun nextMonth() {
        viewModelStateFlow.update {
            it.copy(
                displayMonth = it.displayMonth.plus(1, DateTimeUnit.MONTH),
            )
        }
        paging.clear()
        fetch()
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    public data class ViewModelState(
        val results: List<ApolloResponseState<ApolloResponse<UsageCalendarScreenPagingQuery.Data>>> = listOf(),
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
