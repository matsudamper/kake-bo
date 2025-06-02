package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.subcategory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChartItem
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChartItemEvent
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeMonthlySubCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.PlatformType
import net.matsudamper.money.frontend.common.viewmodel.PlatformTypeProvider
import net.matsudamper.money.frontend.common.viewmodel.ReservedColorModel
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MonthlySubCategoryScreenQuery
import net.matsudamper.money.frontend.graphql.MonthlySubCategoryScreenListQuery
import net.matsudamper.money.frontend.graphql.updateOperation

public class RootHomeMonthlySubCategoryScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    argument: RootHomeScreenStructure.MonthlySubCategory,
    loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    private val navController: ScreenNavController,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val reservedColorModel = ReservedColorModel()

    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            year = argument.year,
            month = argument.month,
            subCategoryId = argument.subCategoryId,
        ),
    )
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val firstQueryFlow = viewModelStateFlow.map {
        it.createFirstQuery()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private fun getFlow(): Flow<ApolloResponse<MonthlySubCategoryScreenListQuery.Data>> {
        @Suppress("OPT_IN_USAGE")
        return firstQueryFlow.filterNotNull().flatMapLatest {
            graphqlClient.apolloClient.query(it)
                .fetchPolicy(FetchPolicy.CacheOnly)
                .watch()
        }
    }

    private val loadedEvent = object : RootHomeMonthlySubCategoryScreenUiState.LoadedEvent {
        override fun loadMore() {
            viewModelScope.launch {
                fetchList()
            }
        }
    }

    public val uiStateFlow: StateFlow<RootHomeMonthlySubCategoryScreenUiState> = MutableStateFlow(
        RootHomeMonthlySubCategoryScreenUiState(
            scaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
                override fun onClickHome() {
                    if (PlatformTypeProvider.type == PlatformType.JS) {
                        super.onClickHome()
                    }
                }
            },
            event = object : RootHomeMonthlySubCategoryScreenUiState.Event {
                override fun onViewInitialized() {
                    viewModelScope.launch {
                        loginCheckUseCase.check()
                    }
                    viewModelScope.launch {
                        getFlow().collectLatest { response ->
                            viewModelStateFlow.update { viewModelState ->
                                viewModelState.copy(
                                    apolloResponse = response,
                                )
                            }
                        }
                    }
                    viewModelScope.launch {
                        fetchSubCategoryName()
                        fetchList()
                    }
                }
            },
            loadingState = RootHomeMonthlySubCategoryScreenUiState.LoadingState.Loading,
            headerTitle = "",
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val listResponse = viewModelState.apolloResponse
                val listData = listResponse?.data?.user?.moneyUsages
                val screenResponse = viewModelState.screenResponse
                val screenResponseData = screenResponse?.data?.user?.moneyUsageSubCategory
                val state = if (listResponse == null || screenResponse == null) {
                    RootHomeMonthlySubCategoryScreenUiState.LoadingState.Loading
                } else if (
                    listResponse.hasErrors() ||
                    listData == null ||
                    screenResponse.hasErrors() ||
                    screenResponseData == null
                ) {
                    RootHomeMonthlySubCategoryScreenUiState.LoadingState.Error
                } else {
                    val items = listData.nodes.map { node -> createItem(node) }
                    val pieChartItems = createPieChartItems(listData.nodes)
                    RootHomeMonthlySubCategoryScreenUiState.LoadingState.Loaded(
                        items = items,
                        event = loadedEvent,
                        hasMoreItem = listResponse.data?.user?.moneyUsages?.hasMore != false,
                        pieChartItems = pieChartItems,
                        pieChartTitle = screenResponseData.name,
                    )
                }
                uiStateFlow.value = uiStateFlow.value.copy(
                    loadingState = state,
                    headerTitle = run {
                        val yearText = "${viewModelState.year}年${viewModelState.month}月"
                        val descriptionText = "サブカテゴリ別一覧"
                        "$yearText $descriptionText"
                    },
                )
            }
        }
    }

    private fun createItem(node: MonthlySubCategoryScreenListQuery.Node): RootHomeMonthlySubCategoryScreenUiState.Item {
        return RootHomeMonthlySubCategoryScreenUiState.Item(
            title = node.title,
            amount = "${Formatter.formatMoney(node.amount)}円",
            category = node.moneyUsageSubCategory?.name.orEmpty(),
            date = Formatter.formatDateTime(node.date),
            event = object : RootHomeMonthlySubCategoryScreenUiState.Item.Event {
                override fun onClick() {
                    viewModelScope.launch {
                        eventSender.send {
                            it.navigate(
                                ScreenStructure.MoneyUsage(
                                    id = node.id,
                                ),
                            )
                        }
                    }
                }
            },
        )
    }

    private fun createPieChartItems(nodes: List<MonthlySubCategoryScreenListQuery.Node>): ImmutableList<PieChartItem> {
        return nodes
            .groupBy { it.moneyUsageSubCategory?.name ?: "その他" }
            .mapValues { (_, nodes) -> nodes.sumOf { it.amount } }
            .filter { it.value > 0 }
            .entries
            .mapIndexed { index, (subCategory, amount) ->
                PieChartItem(
                    color = reservedColorModel.getColor(subCategory),
                    title = subCategory,
                    value = amount.toLong(),
                    event = object : PieChartItemEvent {
                        override fun onClick() {
                        }
                    },
                )
            }.toImmutableList()
    }

    public fun updateStructure(current: RootHomeScreenStructure.MonthlySubCategory) {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                year = current.year,
                month = current.month,
                subCategoryId = current.subCategoryId,
            )
        }
        viewModelScope.launch {
            fetchSubCategoryName()
        }
        viewModelScope.launch {
            fetchList()
        }
    }

    private suspend fun fetchSubCategoryName() {
        val subCategoryId = viewModelStateFlow.value.subCategoryId
        val query = MonthlySubCategoryScreenQuery(id = subCategoryId)
        val response = withContext(Dispatchers.IO) {
            graphqlClient.apolloClient.query(query)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }

        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                screenResponse = response,
            )
        }
    }

    private suspend fun fetchList() {
        val firstQuery = firstQueryFlow.value ?: return
        graphqlClient.apolloClient.updateOperation(firstQuery) update@{ before ->
            if (before == null) {
                return@update success(queryExec(firstQuery))
            }

            if (before.user?.moneyUsages?.hasMore != true) return@update noHasMore()

            val response = queryExec(
                query = viewModelStateFlow.value.createQuery(
                    cursor = before.user?.moneyUsages?.cursor ?: return@update error(),
                ),
            )

            val newNodes = response.data?.user?.moneyUsages?.nodes ?: return@update error()
            val beforeNodes = before.user?.moneyUsages?.nodes ?: return@update error()

            success(
                response.newBuilder()
                    .data(
                        data = before.copy(
                            user = before.user?.copy(
                                moneyUsages = MonthlySubCategoryScreenListQuery.MoneyUsages(
                                    cursor = response.data?.user?.moneyUsages?.cursor,
                                    hasMore = response.data?.user?.moneyUsages?.hasMore ?: false,
                                    nodes = beforeNodes + newNodes,
                                ),
                            ),
                        ),
                    )
                    .build(),
            )
        }
    }

    private suspend fun queryExec(query: MonthlySubCategoryScreenListQuery): ApolloResponse<MonthlySubCategoryScreenListQuery.Data> {
        return withContext(Dispatchers.IO) {
            graphqlClient.apolloClient.query(query)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }
    }

    public interface Event {
        public fun navigate(screen: ScreenStructure)
    }

    private data class ViewModelState(
        val year: Int,
        val month: Int,
        val subCategoryId: MoneyUsageSubCategoryId,
        val screenResponse: ApolloResponse<MonthlySubCategoryScreenQuery.Data>? = null,
        val apolloResponse: ApolloResponse<MonthlySubCategoryScreenListQuery.Data>? = null,
    ) {
        fun createFirstQuery(): MonthlySubCategoryScreenListQuery {
            return createQuery(
                cursor = null,
                subCategoryId = subCategoryId,
                year = year,
                month = month,
            )
        }

        fun createQuery(cursor: String?): MonthlySubCategoryScreenListQuery {
            return createQuery(
                cursor = cursor,
                subCategoryId = subCategoryId,
                year = year,
                month = month,
            )
        }
    }
}

private fun createQuery(
    cursor: String?,
    subCategoryId: MoneyUsageSubCategoryId,
    year: Int,
    month: Int,
): MonthlySubCategoryScreenListQuery {
    val date = LocalDate(
        year = year,
        monthNumber = month,
        dayOfMonth = 1,
    )

    return MonthlySubCategoryScreenListQuery(
        cursor = Optional.present(cursor),
        size = 50,
        isAsc = true,
        subCategory = subCategoryId,
        sinceDateTime = Optional.present(
            LocalDateTime(
                date = date,
                time = LocalTime(0, 0),
            ),
        ),
        untilDateTime = Optional.present(
            LocalDateTime(
                date = date.plus(1, DateTimeUnit.MONTH),
                time = LocalTime(0, 0),
            ),
        ),
    )
}