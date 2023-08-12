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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageCalendarScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class RootUsageCalendarViewModel(
    private val coroutineScope: CoroutineScope,
    private val pagingModel: RootUsageCalendarPagingModel,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    internal val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    public val uiStateFlow: StateFlow<RootUsageCalendarScreenUiState> = MutableStateFlow(
        RootUsageCalendarScreenUiState(
            loadingState = RootUsageCalendarScreenUiState.LoadingState.Loading,
            event = object : RootUsageCalendarScreenUiState.Event {
                override fun onViewInitialized() {
                    pagingModel.fetch()
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
                                        pagingModel.fetch()
                                    }
                                },
                            ),
                        )
                    }
                }
        }
    }.asStateFlow()

    init {
        pagingModel.changeMonth(viewModelStateFlow.value.displayMonth)
        coroutineScope.launch {
            pagingModel.flow.collectLatest { responseStates ->
                viewModelStateFlow.update {
                    it.copy(
                        results = responseStates,
                    )
                }
            }
        }
        coroutineScope.launch {
            pagingModel.flow.map { it.lastOrNull()?.getSuccessOrNull() }
                .collectLatest {
                    if (it?.value?.data?.user?.moneyUsages?.hasMore == true) {
                        pagingModel.fetch()
                    }
                }
        }
    }


    internal fun prevMonth() {
        viewModelStateFlow.update {
            val month = it.displayMonth.minus(1, DateTimeUnit.MONTH)
            pagingModel.changeMonth(month)
            it.copy(
                displayMonth = month,
            )
        }
        pagingModel.fetch()
    }

    internal fun nextMonth() {
        viewModelStateFlow.update {
            val month = it.displayMonth.plus(1, DateTimeUnit.MONTH)
            pagingModel.changeMonth(month)
            it.copy(
                displayMonth = month,
            )
        }
        pagingModel.fetch()
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
