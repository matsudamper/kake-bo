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
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageListPagerHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel

public class RootUsageListPagerHostViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    initial: ScreenStructure.Root.Usage.List,
    private val rootUsageHostViewModel: RootUsageHostViewModel,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            current = initial,
        ),
    )
    private val betweenPageCount = 100
    public val uiState: StateFlow<RootUsageListPagerHostScreenUiState> = MutableStateFlow(
        RootUsageListPagerHostScreenUiState(
            pages = buildList {
                val current = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val initialDate = if (initial.yearMonth != null) {
                    LocalDate(
                        year = initial.yearMonth.year,
                        monthNumber = initial.yearMonth.month,
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
                        RootUsageListPagerHostScreenUiState.Page(
                            navigation = ScreenStructure.Root.Usage.List(
                                yearMonth = ScreenStructure.Root.Usage.List.YearMonth(
                                    year = date.year,
                                    month = date.monthNumber,
                                ),
                            ),
                        ),
                    )
                }
            }.toImmutableList(),
            currentPage = betweenPageCount,
            event = object : RootUsageListPagerHostScreenUiState.Event {
                override fun onPageChanged(page: RootUsageListPagerHostScreenUiState.Page) {
                    rootUsageHostViewModel.updateStructure(page.navigation)
                }
            },
        ),
    ).also { mutableUiStateFlow ->
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

    public fun updateStructure(current: ScreenStructure.Root.Usage.List) {
        viewModelStateFlow.value = ViewModelState(
            current = current,
        )
    }

    public data class ViewModelState(
        val current: ScreenStructure.Root.Usage.List,
    )
}
