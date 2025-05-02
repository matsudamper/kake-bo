package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChartItem
import net.matsudamper.money.frontend.common.ui.screen.root.home.monthly.RootHomeMonthlyScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.PlatformType
import net.matsudamper.money.frontend.common.viewmodel.PlatformTypeProvider
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.common.viewmodel.root.home.RootHomeTabScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.RootHomeMonthlyScreenViewModel.ViewModelState.SortState
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MonthlyScreenListQuery
import net.matsudamper.money.frontend.graphql.MonthlyScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQueryOrderType
import net.matsudamper.money.frontend.graphql.updateOperation

public class RootHomeMonthlyScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    argument: RootHomeScreenStructure.Monthly,
    loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    private val graphqlClient: GraphqlClient,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val reservedColorModel = ReservedColorModel()

    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            argument = argument,
        ),
    )

    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val tabViewModel = RootHomeTabScreenViewModel(
        scopedObjectFeature = scopedObjectFeature,
        loginCheckUseCase = loginCheckUseCase,
    ).also { viewModel ->
        viewModelScope.launch {
            viewModel.viewModelEventHandler.collect(
                object : RootHomeTabScreenViewModel.Event {
                    override fun navigate(screen: ScreenStructure) {
                        viewModelScope.launch { eventSender.send { it.navigate(screen) } }
                    }
                },
            )
        }
    }
    private val loadedEvent = object : RootHomeMonthlyScreenUiState.LoadedEvent {
        override fun loadMore() {
            viewModelScope.launch { fetch() }
        }
    }

    public val uiStateFlow: StateFlow<RootHomeMonthlyScreenUiState> = MutableStateFlow(
        RootHomeMonthlyScreenUiState(
            loadingState = RootHomeMonthlyScreenUiState.LoadingState.Loading,
            rootHomeTabUiState = tabViewModel.uiStateFlow.value,
            currentSortType = RootHomeMonthlyScreenUiState.SortType.Date,
            sortOrder = RootHomeMonthlyScreenUiState.SortOrder.Ascending,
            scaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
                override fun onClickHome() {
                    if (PlatformTypeProvider.type == PlatformType.JS) {
                        super.onClickHome()
                    }
                }
            },
            event = object : RootHomeMonthlyScreenUiState.Event {
                override suspend fun onViewInitialized() {
                    viewModelScope.launch {
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
                            graphqlClient.apolloClient.query(
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
                    viewModelScope.launch {
                        getFirstQueryFlow().collectLatest { firstQuery ->
                            graphqlClient.apolloClient.query(firstQuery)
                                .fetchPolicy(FetchPolicy.CacheFirst)
                                .watch()
                                .collectLatest { response ->
                                    viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                        monthlyListResponse = ApolloResponseState.Success(response),
                                    )
                                }
                        }
                    }
                }

                override fun onSortTypeChanged(sortType: RootHomeMonthlyScreenUiState.SortType) {
                    updateSortType(sortType)
                }

                override fun onSortOrderChanged(order: RootHomeMonthlyScreenUiState.SortOrder) {
                    setOrder(
                        type = viewModelStateFlow.value.sortStateMap.currentSortType,
                        order = order,
                    )
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            tabViewModel.uiStateFlow.collectLatest { rootHomeTabUiState ->
                uiStateFlow.value = uiStateFlow.value.copy(
                    rootHomeTabUiState = rootHomeTabUiState,
                )
            }
        }
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.value = uiStateFlow.value.copy(
                    loadingState = when (viewModelState.monthlyListResponse) {
                        is ApolloResponseState.Loading -> {
                            RootHomeMonthlyScreenUiState.LoadingState.Loading
                        }

                        is ApolloResponseState.Failure -> {
                            RootHomeMonthlyScreenUiState.LoadingState.Error
                        }

                        is ApolloResponseState.Success -> {
                            createLoadedUiState(
                                viewModelState = viewModelState,
                            )
                        }
                    },
                    currentSortType = viewModelState.sortStateMap.currentSortState.type,
                    sortOrder = viewModelState.sortStateMap.currentSortState.order,
                )
            }
        }
    }.asStateFlow()

    private fun createLoadedUiState(viewModelState: ViewModelState): RootHomeMonthlyScreenUiState.LoadingState.Loaded {
        val pieChartItems = viewModelState.moneyUsageAnalytics?.byCategories?.mapIndexed { index, byCategory ->
            PieChartItem(
                title = byCategory.category.name,
                color = reservedColorModel.getColor(byCategory.category.name),
                value = byCategory.totalAmount ?: 0,
            )
        }.orEmpty().sortedByDescending { it.value }

        val response = (viewModelState.monthlyListResponse as? ApolloResponseState.Success)?.value
        val nodes = response?.data?.user?.moneyUsages?.nodes.orEmpty()

        return RootHomeMonthlyScreenUiState.LoadingState.Loaded(
            items = nodes.map { node ->
                RootHomeMonthlyScreenUiState.Item(
                    title = node.title,
                    amount = "${Formatter.formatMoney(node.amount)}円",
                    date = Formatter.formatDateTime(node.date),
                    category = node.moneyUsageSubCategory?.name.orEmpty(),
                    event = ItemEventImpl(
                        coroutineScope = viewModelScope,
                        eventSender = eventSender,
                        id = node.id,
                    ),
                )
            }.toImmutableList(),
            pieChartItems = pieChartItems.toImmutableList(),
            hasMoreItem = response?.data?.user?.moneyUsages?.hasMore != false,
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

        viewModelScope.launch {
            fetch()
        }
    }

    private suspend fun fetch() {
        val firstQuery = getFirstQueryFlow().value
        graphqlClient.apolloClient.updateOperation(firstQuery) update@{ before ->
            if (before == null) return@update success(fetch(firstQuery = firstQuery, cursor = null))
            if (before.user?.moneyUsages?.hasMore == false) return@update noHasMore()

            val cursor = before.user?.moneyUsages?.cursor

            val result = fetch(firstQuery = firstQuery, cursor = cursor)
            val newMoneyUsage = result.data?.user?.moneyUsages ?: return@update error()
            success(
                result.newBuilder()
                    .data(
                        data = before.copy(
                            user = before.user?.let user@{ user ->
                                val usages = user.moneyUsages ?: return@user null
                                user.copy(
                                    moneyUsages = MonthlyScreenListQuery.MoneyUsages(
                                        cursor = newMoneyUsage.cursor,
                                        hasMore = newMoneyUsage.hasMore,
                                        nodes = usages.nodes + newMoneyUsage.nodes,
                                    ),
                                )
                            },
                        ),
                    )
                    .build(),
            )
        }
    }

    private suspend fun fetch(firstQuery: MonthlyScreenListQuery, cursor: String?): ApolloResponse<MonthlyScreenListQuery.Data> {
        return graphqlClient.apolloClient.query(
            firstQuery.copy(cursor = Optional.present(cursor)),
        ).execute()
    }

    private fun createSinceLocalDateTime(): LocalDateTime {
        val tmp = viewModelStateFlow.value.argument.date
            ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        return LocalDateTime(
            date = LocalDate(
                year = tmp.year,
                monthNumber = tmp.monthNumber,
                dayOfMonth = 1,
            ),
            time = LocalTime(0, 0),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getFirstQueryFlow(): StateFlow<MonthlyScreenListQuery> {
        return viewModelStateFlow.mapLatest { viewModelState ->
            MonthlyScreenListQuery(
                cursor = Optional.present(null),
                size = 5,
                sinceDateTime = Optional.present(createSinceLocalDateTime()),
                untilDateTime = Optional.present(
                    LocalDateTime(
                        date = createSinceLocalDateTime().date.plus(1, DateTimeUnit.MONTH),
                        time = createSinceLocalDateTime().time,
                    ),
                ),
                isAsc = when (viewModelState.sortStateMap.currentSortState.order) {
                    RootHomeMonthlyScreenUiState.SortOrder.Ascending -> true
                    RootHomeMonthlyScreenUiState.SortOrder.Descending -> false
                },
                orderType = Optional.present(
                    when (viewModelState.sortStateMap.currentSortState.type) {
                        RootHomeMonthlyScreenUiState.SortType.Date -> {
                            MoneyUsagesQueryOrderType.DATE
                        }

                        RootHomeMonthlyScreenUiState.SortType.Amount -> {
                            MoneyUsagesQueryOrderType.AMOUNT
                        }
                    },
                ),
            )
        }.stateIn(CoroutineScope(coroutineContext))
    }

    private fun updateSortType(
        type: RootHomeMonthlyScreenUiState.SortType,
    ) {
        viewModelStateFlow.update {
            it.copy(
                sortStateMap = it.sortStateMap.copy(
                    currentSortType = type,
                ),
            )
        }
    }

    private fun setOrder(
        type: RootHomeMonthlyScreenUiState.SortType,
        order: RootHomeMonthlyScreenUiState.SortOrder,
    ) {
        viewModelStateFlow.update {
            it.copy(
                sortStateMap = it.sortStateMap.copy(
                    sortStateList = it.sortStateMap.sortStateList.toMutableMap().also { map ->
                        map[type] = map.getOrPut(key = type) {
                            SortState(
                                type = type,
                                order = order,
                            )
                        }.copy(
                            order = order,
                        )
                    },
                ),
            )
        }
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val argument: RootHomeScreenStructure.Monthly,
        val monthlyListResponse: ApolloResponseState<ApolloResponse<MonthlyScreenListQuery.Data>> = ApolloResponseState.Loading(),
        val moneyUsageAnalytics: MonthlyScreenQuery.MoneyUsageAnalytics? = null,
        val sortStateMap: SortStateMap = SortStateMap(),
    ) {
        data class SortStateMap(
            val sortStateList: Map<RootHomeMonthlyScreenUiState.SortType, SortState> = mapOf(),
            val currentSortType: RootHomeMonthlyScreenUiState.SortType = RootHomeMonthlyScreenUiState.SortType.Date,
        ) {
            val currentSortState: SortState = run state@{
                val current = sortStateList[currentSortType]
                if (current != null) return@state current

                SortState(
                    type = currentSortType,
                    order = when (currentSortType) {
                        RootHomeMonthlyScreenUiState.SortType.Date -> {
                            RootHomeMonthlyScreenUiState.SortOrder.Ascending
                        }

                        RootHomeMonthlyScreenUiState.SortType.Amount -> {
                            RootHomeMonthlyScreenUiState.SortOrder.Descending
                        }
                    },
                )
            }
        }

        data class SortState(
            val type: RootHomeMonthlyScreenUiState.SortType,
            val order: RootHomeMonthlyScreenUiState.SortOrder,
        )
    }
}
