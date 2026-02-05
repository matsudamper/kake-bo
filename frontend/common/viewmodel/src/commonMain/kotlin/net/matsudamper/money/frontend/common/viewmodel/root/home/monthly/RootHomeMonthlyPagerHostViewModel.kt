package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly

import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.ui.screen.root.home.monthly.RootHomeMonthlyPagerHostScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.PlatformType
import net.matsudamper.money.frontend.common.viewmodel.PlatformTypeProvider
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl

public class RootHomeMonthlyPagerHostViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    initial: RootHomeScreenStructure.Monthly,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            current = initial,
        ),
    )
    private val betweenPageCount = 100
    public val uiState: StateFlow<RootHomeMonthlyPagerHostScreenUiState> = MutableStateFlow(
        RootHomeMonthlyPagerHostScreenUiState(
            scaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
                override fun onClickHome() {
                    if (PlatformTypeProvider.type == PlatformType.JS) {
                        super.onClickHome()
                    }
                }
            },
            pages = buildList {
                val current = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val initialDate = initial.date ?: current
                for (index in -betweenPageCount..betweenPageCount) {
                    add(
                        RootHomeMonthlyPagerHostScreenUiState.Page(
                            RootHomeScreenStructure.Monthly(
                                initialDate.plus(
                                    value = index,
                                    unit = DateTimeUnit.MONTH,
                                ),
                            ),
                        ),
                    )
                }
            }.toImmutableList(),
            currentPage = betweenPageCount,
        ),
    ).also { mutableUiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                mutableUiStateFlow.update { uiState ->
                    uiState.copy(
                        currentPage = uiState.pages.indexOfFirst { page ->
                            page.navigation.date == viewModelState.current.date
                        }.takeIf { it >= 0 } ?: TODO("範囲外: ${viewModelState.current.date}"),
                    )
                }
            }
        }
    }.asStateFlow()

    public fun updateStructure(current: RootHomeScreenStructure.Monthly) {
        viewModelStateFlow.value = ViewModelState(
            current = current,
        )
    }

    public data class ViewModelState(
        val current: RootHomeScreenStructure.Monthly,
    )
}
