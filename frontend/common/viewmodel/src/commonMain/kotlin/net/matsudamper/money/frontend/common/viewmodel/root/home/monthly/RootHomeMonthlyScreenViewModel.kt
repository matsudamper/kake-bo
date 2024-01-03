package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.monthly.RootHomeMonthlyScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenViewModel
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MonthlyScreenListQuery
import net.matsudamper.money.frontend.graphql.MonthlyScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class RootHomeMonthlyScreenViewModel(
    private val coroutineScope: CoroutineScope,
    argument: RootHomeScreenStructure.Monthly,
    loginCheckUseCase: LoginCheckUseCase,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            argument = argument,
        ),
    )
    private val monthlyListState: ApolloPagingResponseCollector<MonthlyScreenListQuery.Data> = ApolloPagingResponseCollector.create(
        apolloClient = apolloClient,
        coroutineScope = coroutineScope,
    )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val tabViewModel = RootHomeTabScreenViewModel(
        coroutineScope = coroutineScope,
        loginCheckUseCase = loginCheckUseCase,
    ).also { viewModel ->
        coroutineScope.launch {
            viewModel.viewModelEventHandler.collect(
                object : RootHomeTabScreenViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        coroutineScope.launch { eventSender.send { it.navigate(screen) } }
                    }
                },
            )
        }
    }
    private val loadedEvent = object : RootHomeMonthlyScreenUiState.LoadedEvent {
        override fun loadMore() {
            coroutineScope.launch { fetch() }
        }
    }
    public val uiStateFlow: StateFlow<RootHomeMonthlyScreenUiState> = MutableStateFlow(
        RootHomeMonthlyScreenUiState(
            loadingState = RootHomeMonthlyScreenUiState.LoadingState.Loading,
            rootHomeTabUiState = tabViewModel.uiStateFlow.value,
            event = object : RootHomeMonthlyScreenUiState.Event {
                override suspend fun onViewInitialized() {
                    coroutineScope.launch {
                        viewModelStateFlow.map { viewModelState ->
                            viewModelState.argument
                        }.stateIn(this).collectLatest {
                            val sinceDateTime = createSinceLocalDateTime()
                            val untilDateTime = run {
                                LocalDateTime(
                                    date = sinceDateTime.date.plus(1, DateTimeUnit.MONTH),
                                    time = sinceDateTime.time,
                                )
                            }
                            apolloClient.query(
                                MonthlyScreenQuery(
                                    sinceDateTime = sinceDateTime,
                                    untilDateTime = untilDateTime,
                                ),
                            ).fetchPolicy(FetchPolicy.CacheFirst).toFlow().collectLatest { response ->
                                viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                    moneyUsageAnalytics = response.data?.user?.moneyUsageAnalytics,
                                )
                            }
                        }
                    }
                    coroutineScope.launch {
                        monthlyListState.flow.collectLatest { responses ->
                            viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                monthlyListResponses = responses,
                            )
                        }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            tabViewModel.uiStateFlow.collectLatest { rootHomeTabUiState ->
                uiStateFlow.value = uiStateFlow.value.copy(
                    rootHomeTabUiState = rootHomeTabUiState,
                )
            }
        }
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.value = uiStateFlow.value.copy(
                    loadingState = when (viewModelState.monthlyListResponses.firstOrNull()) {
                        null,
                        is ApolloResponseState.Loading,
                        -> RootHomeMonthlyScreenUiState.LoadingState.Loading

                        is ApolloResponseState.Failure -> RootHomeMonthlyScreenUiState.LoadingState.Error

                        is ApolloResponseState.Success -> {
                            createLoadedUiState(
                                viewModelState = viewModelState,
                            )
                        }
                    },
                )
            }
        }
    }.asStateFlow()

    private fun createLoadedUiState(viewModelState: ViewModelState): RootHomeMonthlyScreenUiState.LoadingState.Loaded {
        return RootHomeMonthlyScreenUiState.LoadingState.Loaded(
            items = viewModelState.monthlyListResponses.flatMap {
                it.getSuccessOrNull()?.value?.data?.user?.moneyUsages?.nodes.orEmpty()
            }.map { node ->
                RootHomeMonthlyScreenUiState.Item(
                    title = node.title,
                    amount = "${Formatter.formatMoney(node.amount)}円",
                    date = Formatter.formatDateTime(node.date),
                    category = node.moneyUsageSubCategory?.name.orEmpty(),
                    event = ItemEventImpl(
                        coroutineScope = coroutineScope,
                        eventSender = eventSender,
                        id = node.id,
                    ),
                )
            },
            hasMoreItem = viewModelState.monthlyListResponses
                .lastOrNull()?.getSuccessOrNull()?.value?.data?.user?.moneyUsages?.hasMore != false,
            totalAmount = run amount@{
                val totalAmount = viewModelState.moneyUsageAnalytics?.totalAmount ?: return@amount ""
                "${Formatter.formatMoney(totalAmount)}円"
            },
            event = loadedEvent,
        )
    }

    private data class ItemEventImpl(
        private val coroutineScope: CoroutineScope,
        private val eventSender: EventSender<Event>,
        private val id: MoneyUsageId,
    ) : RootHomeMonthlyScreenUiState.ItemEvent {
        override fun onClick() {
            coroutineScope.launch {
                eventSender.send {
                    it.navigate(
                        ScreenStructure.MoneyUsage(
                            id = id,
                        ),
                    )
                }
            }
        }
    }

    public fun updateStructure(current: RootHomeScreenStructure.Monthly) {
        viewModelStateFlow.value = viewModelStateFlow.value.copy(argument = current)
        tabViewModel.updateScreenStructure(current)

        monthlyListState.clear()
        coroutineScope.launch {
            fetch()
        }
    }

    private suspend fun fetch() {
        monthlyListState.add {
            val cursor: String? = when (val lastState = monthlyListState.lastValue.lastOrNull()) {
                is ApolloResponseState.Loading -> return@add null
                is ApolloResponseState.Failure -> {
                    coroutineScope.launch {
                        monthlyListState.lastRetry()
                    }
                    return@add null
                }

                null -> null

                is ApolloResponseState.Success -> {
                    val result = lastState.value.data?.user?.moneyUsages
                    if (result == null) {
                        coroutineScope.launch {
                            monthlyListState.lastRetry()
                        }
                        return@add null
                    }
                    if (result.hasMore.not()) return@add null

                    result.cursor
                }
            }
            val sinceDateTime = createSinceLocalDateTime()
            val untilDateTime = run {
                LocalDateTime(
                    date = sinceDateTime.date.plus(1, DateTimeUnit.MONTH),
                    time = sinceDateTime.time,
                )
            }
            MonthlyScreenListQuery(
                cursor = Optional.present(cursor),
                size = 50,
                sinceDateTime = Optional.present(sinceDateTime),
                untilDateTime = Optional.present(untilDateTime),
            )
        }
    }

    private fun createSinceLocalDateTime(): LocalDateTime {
        val tmp = (
            viewModelStateFlow.value.argument.date
                ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            )

        return LocalDateTime(
            date = LocalDate(
                year = tmp.year,
                monthNumber = tmp.monthNumber,
                dayOfMonth = 1,
            ),
            time = LocalTime(0, 0),
        )
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val argument: RootHomeScreenStructure.Monthly,
        val monthlyListResponses: List<ApolloResponseState<ApolloResponse<MonthlyScreenListQuery.Data>>> = listOf(),
        val moneyUsageAnalytics: MonthlyScreenQuery.MoneyUsageAnalytics? = null,
    )
}
