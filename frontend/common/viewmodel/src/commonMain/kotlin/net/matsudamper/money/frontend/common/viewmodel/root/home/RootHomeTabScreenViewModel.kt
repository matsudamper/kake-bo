package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
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
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeTabUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.RootHomeTabScreenQuery

public class RootHomeTabScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: RootHomeTabScreenApi,
    private val loginCheckUseCase: LoginCheckUseCase,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val uiStateEvent = object : RootHomeTabUiState.Event {
        override fun onViewInitialized() {
            fetch()
        }
    }
    private val uiStateLoadedEvent = object : RootHomeTabUiState.LoadedEvent {
        override fun onClickMailImportButton() {
            coroutineScope.launch {
                viewModelEventSender.send { it.navigateToMailImport() }
            }
        }

        override fun onClickNotLinkedMailButton() {
            coroutineScope.launch {
                viewModelEventSender.send { it.navigateToMailLink() }
            }
        }
    }

    private val betweenEvent = object : RootHomeTabUiState.BetweenEvent {
        override fun onClickNextMonth() {
            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = it.displayPeriod.copy(
                        sinceDate = it.displayPeriod.sinceDate.addMonth(1),
                    ),
                )
            }
            fetch()
        }

        override fun onClickPreviousMonth() {
            viewModelStateFlow.update {
                it.copy(
                    displayPeriod = it.displayPeriod.copy(
                        sinceDate = it.displayPeriod.sinceDate.addMonth(-1),
                    ),
                )
            }
            fetch()
        }
    }

    public val uiStateFlow: StateFlow<RootHomeTabUiState> = MutableStateFlow(
        RootHomeTabUiState(
            screenState = RootHomeTabUiState.ScreenState.Loading,
            event = uiStateEvent,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val screenState = run screenState@{
                    val displayPeriods = (0 until viewModelState.displayPeriod.monthCount).map { index ->
                        viewModelState.displayPeriod.sinceDate.addMonth(index)
                    }
                    val allLoaded = displayPeriods.all { displayPeriod ->
                        viewModelState.responseMap.contains(displayPeriod)
                    }
                    if (allLoaded.not()) {
                        return@screenState RootHomeTabUiState.ScreenState.Loading
                    }

                    val responses = run {
                        val responses = displayPeriods.map { displayPeriod ->
                            viewModelState.responseMap[displayPeriod]
                        }
                        if (responses.size != responses.filterNotNull().size) {
                            return@screenState RootHomeTabUiState.ScreenState.Error
                        }
                        displayPeriods.zip(responses.filterNotNull())
                    }

                    RootHomeTabUiState.ScreenState.Loaded(
                        displayType = RootHomeTabUiState.DisplayType.Between(
                            between = "${displayPeriods.first().year}/${displayPeriods.first().month} - ${displayPeriods.last().year}/${displayPeriods.last().month}",
                            event = betweenEvent,
                            totals = responses.map { (yearMonth, response) ->
                                RootHomeTabUiState.Total(
                                    amount = response.data?.user?.moneyUsageStatics?.totalAmount
                                        ?: return@screenState RootHomeTabUiState.ScreenState.Error,
                                    year = yearMonth.year,
                                    month = yearMonth.month,
                                )
                            }.toImmutableList(),
                        ),
                        event = uiStateLoadedEvent,
                    )
                }

                uiStateFlow.update { uiState ->
                    uiState.copy(
                        screenState = screenState,
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            loginCheckUseCase.check()
        }
    }

    private fun fetch() {
        coroutineScope.launch {
            val period = viewModelStateFlow.value.displayPeriod
            (0 until period.monthCount)
                .map { index ->
                    val start = period.sinceDate.addMonth(index)

                    start to start.addMonth(index + 1)
                }
                .filter { (startYearMonth, _) ->
                    viewModelStateFlow.value.responseMap.contains(startYearMonth).not()
                }
                .map { (startYearMonth, endYearMonth) ->
                    launch {
                        val result = api.fetch(
                            startYear = startYearMonth.year,
                            startMonth = startYearMonth.month,
                            endYear = endYearMonth.year,
                            endMonth = endYearMonth.month,
                        )

                        viewModelStateFlow.update {
                            it.copy(
                                responseMap = it.responseMap
                                    .plus(startYearMonth to result.getOrNull()),
                            )
                        }
                    }
                }
        }
    }

    public interface Event {
        public fun navigateToMailImport()
        public fun navigateToMailLink()
    }

    private data class ViewModelState(
        val responseMap: Map<YearMonth, ApolloResponse<RootHomeTabScreenQuery.Data>?> = mapOf(),
        val displayPeriod: Period = run {
            val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            Period(
                sinceDate = YearMonth(currentDate.year, currentDate.monthNumber),
                monthCount = 3,
            )
        },
    ) {
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
