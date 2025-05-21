package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabPeriodSubCategoryContentUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.PlatformType
import net.matsudamper.money.frontend.common.viewmodel.PlatformTypeProvider
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender

public class RootHomeTabPeriodSubCategoryContentViewModel(
    initialSubCategoryId: MoneyUsageSubCategoryId,
    scopedObjectFeature: ScopedObjectFeature,
    loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val reservedColorModel = ReservedColorModel()
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            subCategoryId = initialSubCategoryId,
        ),
    )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    public val uiStateFlow: StateFlow<RootHomeTabPeriodSubCategoryContentUiState> = MutableStateFlow(
        RootHomeTabPeriodSubCategoryContentUiState(
            loadingState = RootHomeTabPeriodSubCategoryContentUiState.LoadingState.Loading,
            rootScaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
                override fun onClickHome() {
                    if (PlatformTypeProvider.type == PlatformType.JS) {
                        super.onClickHome()
                    } else {
                        // TODO scroll to top
                    }
                }
            },
            event = object : RootHomeTabPeriodSubCategoryContentUiState.Event {
                override suspend fun onViewInitialized() {
                    viewModelScope.launch {
                        loginCheckUseCase.check()
                    }
                }

                override fun refresh() {
                    TODO("Not yet implemented")
                }
            },
        ),
    ).also { mutableUiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                mutableUiStateFlow.update {
                    TODO()
                }
            }
        }
    }.asStateFlow()

    private data class ViewModelState(
        val subCategoryId: MoneyUsageSubCategoryId,
        val displayPeriod: Period = run {
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            Period(
                sinceDate = YearMonth(currentDate.year, currentDate.monthNumber).addMonth(-5),
                monthCount = 6,
            )
        },
    ) {
        data class YearMonthCategory(
            val categoryId: MoneyUsageCategoryId,
            val yearMonth: YearMonth,
        )

        data class YearMonth(
            val year: Int,
            val month: Int,
        ) {
            fun addMonth(count: Int): YearMonth {
                val nextDate = LocalDate(year, month, 1)
                    .plus(count, DateTimeUnit.MONTH)

                return YearMonth(
                    year = nextDate.year,
                    month = nextDate.monthNumber,
                )
            }
        }

        data class Period(
            val sinceDate: YearMonth,
            val monthCount: Int,
        )
    }
}
