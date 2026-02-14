package net.matsudamper.money.frontend.common.viewmodel.root.usage

import androidx.compose.ui.graphics.Color
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.UsageCalendarScreenPagingQuery

public class MoneyUsagesCalendarViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val rootUsageHostViewModel: RootUsageHostViewModel,
    private val calendarPagingModel: RootUsageCalendarPagingModel,
    yearMonth: ScreenStructure.Root.Usage.Calendar.YearMonth?,
) : CommonViewModel(scopedObjectFeature) {
    private val reservedColorModel = ReservedColorModel()

    internal val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            displayMonth = createDisplayMonth(yearMonth),
        ),
    )

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    public val uiStateFlow: StateFlow<RootUsageCalendarScreenUiState> = MutableStateFlow(
        RootUsageCalendarScreenUiState(
            loadingState = RootUsageCalendarScreenUiState.LoadingState.Loading,
            event = object : RootUsageCalendarScreenUiState.Event {
                override suspend fun onViewInitialized() {
                    if (calendarPagingModel.hasSelectedMonth().not()) {
                        calendarPagingModel.changeMonth(viewModelStateFlow.value.displayMonth)
                    }
                    CoroutineScope(currentCoroutineContext()).launch {
                        launch {
                            calendarPagingModel.getFlow()
                                .collectLatest {
                                    calendarPagingModel.fetch()
                                }
                        }
                        launch {
                            calendarPagingModel.getFlow().collectLatest { responseStates ->
                                viewModelStateFlow.update {
                                    it.copy(
                                        response = responseStates,
                                    )
                                }
                            }
                        }

                        launch {
                            calendarPagingModel.fetch()
                        }
                        launch {
                            rootUsageHostViewModel.viewModelStateFlow.collectLatest { rootViewModelState ->
                                delay(100)
                                calendarPagingModel.changeSearchText(
                                    text = rootViewModelState.searchText,
                                )
                                calendarPagingModel.changeCategoryId(
                                    categoryId = rootViewModelState.selectedCategoryId,
                                )
                                calendarPagingModel.changeSubCategoryId(
                                    subCategoryId = rootViewModelState.selectedSubCategoryId,
                                )
                            }
                        }
                    }
                }

                override fun refresh() {
                    viewModelScope.launch {
                        calendarPagingModel.refresh()
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow
                .collectLatest { viewModelState ->
                    val nodes = viewModelState.response?.data?.user?.moneyUsages?.nodes.orEmpty()
                        .filter { node ->
                            node.date.date.month == viewModelState.displayMonth.month
                        }

                    val cells: ImmutableList<RootUsageCalendarScreenUiState.CalendarCell> = run cells@{
                        val dayGroup = nodes.groupBy { it.date.date.dayOfMonth }

                        val firstDay = viewModelState.displayMonth
                            .let { LocalDate(it.year, it.monthNumber, 1) }

                        val daysOfMonth = run daysOfMonth@{
                            buildList<LocalDate> {
                                add(firstDay)
                                while (last().month == firstDay.month) {
                                    add(last().plus(1, DateTimeUnit.DAY))
                                }
                                removeAt(lastIndex)
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
                                        event = object : RootUsageCalendarScreenUiState.DayCellEvent {
                                            override fun onClick() {
                                                viewModelScope.launch {
                                                    viewModelEventSender.send {
                                                        it.navigate(
                                                            ScreenStructure.CalendarDateList(
                                                                year = localDate.year,
                                                                month = localDate.monthNumber,
                                                                day = localDate.dayOfMonth,
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        items = days.map { day ->
                                            val subCategory = day.moneyUsageSubCategory
                                            val color = if (subCategory != null) {
                                                reservedColorModel.getColor(
                                                    subCategory.id.toString(),
                                                    subCategory.category.color,
                                                )
                                            } else {
                                                Color.Gray
                                            }
                                            RootUsageCalendarScreenUiState.CalendarDayItem(
                                                title = day.title,
                                                color = color,
                                                event = object : RootUsageCalendarScreenUiState.CalendarDayEvent {
                                                    override fun onClick() {
                                                        viewModelScope.launch {
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
                                        viewModelScope.launch {
                                            calendarPagingModel.fetch()
                                        }
                                    }
                                },
                            ),
                        )
                    }
                }
        }
    }.asStateFlow()

    private fun createDisplayMonth(yearMonth: ScreenStructure.Root.Usage.Calendar.YearMonth?): LocalDate {
        return if (yearMonth != null) {
            LocalDate(
                year = yearMonth.year,
                month = yearMonth.month,
                day = 1,
            )
        } else {
            val currentLocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            LocalDate(
                year = currentLocalDateTime.year,
                month = currentLocalDateTime.month,
                day = 1,
            )
        }
    }

    public data class ViewModelState(
        val response: ApolloResponse<UsageCalendarScreenPagingQuery.Data>? = null,
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

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }
}
