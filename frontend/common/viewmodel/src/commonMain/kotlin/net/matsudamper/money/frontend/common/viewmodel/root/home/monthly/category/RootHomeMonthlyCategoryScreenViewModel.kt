package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.internal.JSJoda.YearMonth
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeMonthlyCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenViewModel

public class RootHomeMonthlyCategoryScreenViewModel(
    private val coroutineScope: CoroutineScope,
    argument: RootHomeScreenStructure.MonthlyCategory,
    loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            yearMonth = argument.yearMonth,
            categoryId = argument.categoryId,
        ),
    )
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiStateFlow: StateFlow<RootHomeMonthlyCategoryScreenUiState> = MutableStateFlow(
        RootHomeMonthlyCategoryScreenUiState(
            event = object : RootHomeMonthlyCategoryScreenUiState.Event {
                override fun onViewInitialized() {
                }
            },
            loadingState = RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading,
            title = "${argument.yearMonth.year()}-${argument.yearMonth.monthValue()} カテゴリ別一覧",
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
        }
    }

    public fun updateStructure(current: RootHomeScreenStructure.MonthlyCategory) {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                yearMonth = current.yearMonth,
            )
        }
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val yearMonth: YearMonth,
        val categoryId: MoneyUsageCategoryId,
    )
}
