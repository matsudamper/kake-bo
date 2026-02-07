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
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageCalendarPagerHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel

public class RootUsageCalendarPagerHostViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    initial: ScreenStructure.Root.Usage.Calendar,
    private val rootUsageHostViewModel: RootUsageHostViewModel,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            current = initial,
        ),
    )
    private val betweenPageCount = 100
    public val uiState: StateFlow<RootUsageCalendarPagerHostScreenUiState> = MutableStateFlow(
        RootUsageCalendarPagerHostScreenUiState(
            pages = buildList {
                val current = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val yearMonth = initial.yearMonth
                val initialDate = if (yearMonth != null) {
                    LocalDate(
                        year = yearMonth.year,
                        monthNumber = yearMonth.month,
                        dayOfMonth = 1,
                    )
                } else {
                    LocalDate(
                        year = current.year,
                        monthNumber = current.monthNumber,
                        dayOfMonth = 1,
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
                                    month = date.monthNumber,
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
                    rootUsageHostViewModel.updateStructure(page.navigation)
                }
            },
        ),
    ).also { mutableUiStateFlow ->
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
                            page.navigation.yearMonth == viewModelState.current.yearMonth
                        }.takeIf { it >= 0 } ?: uiState.currentPage,
                    )
                }
            }
        }
    }.asStateFlow()

    public fun updateStructure(current: ScreenStructure.Root.Usage.Calendar) {
        viewModelStateFlow.value = ViewModelState(
            current = current,
        )
    }

    public data class ViewModelState(
        val current: ScreenStructure.Root.Usage.Calendar,
    )
}
