package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.category

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
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChartItem
import net.matsudamper.money.frontend.common.ui.layout.graph.pie.PieChartItemEvent
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeMonthlyCategoryScreenUiState
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
import net.matsudamper.money.frontend.graphql.MonthlyCategoryScreenListQuery
import net.matsudamper.money.frontend.graphql.MonthlyCategoryScreenQuery
import net.matsudamper.money.frontend.graphql.updateOperation

public class RootHomeMonthlyCategoryScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    argument: RootHomeScreenStructure.MonthlyCategory,
    loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    private val navController: ScreenNavController,
    private val graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val reservedColorModel = ReservedColorModel()

    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            year = argument.year,
            month = argument.month,
            categoryId = argument.categoryId,
        ),
    )
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val firstQueryFlow = viewModelStateFlow.map {
        it.createFirstQuery()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private fun getFlow(): Flow<ApolloResponse<MonthlyCategoryScreenListQuery.Data>> {
        @Suppress("OPT_IN_USAGE")
        return firstQueryFlow.filterNotNull().flatMapLatest {
            graphqlClient.apolloClient.query(it)
                .fetchPolicy(FetchPolicy.CacheOnly)
                .watch()
        }
    }

    private val loadedEvent = object : RootHomeMonthlyCategoryScreenUiState.LoadedEvent {
        override fun loadMore() {
            viewModelScope.launch {
                fetchList()
            }
        }
    }

    public val uiStateFlow: StateFlow<RootHomeMonthlyCategoryScreenUiState> = MutableStateFlow(
        RootHomeMonthlyCategoryScreenUiState(
            scaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
                override fun onClickHome() {
                    if (PlatformTypeProvider.type == PlatformType.JS) {
                        super.onClickHome()
                    }
                }
            },
            event = object : RootHomeMonthlyCategoryScreenUiState.Event {
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
                        fetchCategoryName()
                        fetchList()
                    }
                }
            },
            loadingState = RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading,
            headerTitle = "",
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                val response = viewModelState.apolloResponse
                val state = if (response == null) {
                    RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading
                } else if (response.hasErrors()) {
                    RootHomeMonthlyCategoryScreenUiState.LoadingState.Error
                } else {
                    val nodes = response.data?.user?.moneyUsages?.nodes.orEmpty()
                    val items = nodes.map { node -> createItem(node) }
                    val pieChartItems = createPieChartItems(nodes)
                    RootHomeMonthlyCategoryScreenUiState.LoadingState.Loaded(
                        items = items,
                        event = loadedEvent,
                        hasMoreItem = response.data?.user?.moneyUsages?.hasMore != false,
                        pieChartItems = pieChartItems,
                        pieChartTitle = viewModelState.categoryName ?: "カテゴリ別一覧",
                    )
                }
                uiStateFlow.value = uiStateFlow.value.copy(
                    loadingState = state,
                    headerTitle = run {
                        val yearText = "${viewModelState.year}年${viewModelState.month}月"
                        val descriptionText = viewModelState.categoryName ?: "カテゴリ別一覧"
                        "$yearText $descriptionText"
                    },
                )
            }
        }
    }

    private fun createItem(node: MonthlyCategoryScreenListQuery.Node): RootHomeMonthlyCategoryScreenUiState.Item {
        return RootHomeMonthlyCategoryScreenUiState.Item(
            title = node.title,
            amount = "${Formatter.formatMoney(node.amount)}円",
            subCategory = node.moneyUsageSubCategory?.name.orEmpty(),
            date = Formatter.formatDateTime(node.date),
            event = object : RootHomeMonthlyCategoryScreenUiState.Item.Event {
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

    private fun createPieChartItems(nodes: List<MonthlyCategoryScreenListQuery.Node>): ImmutableList<PieChartItem> {
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
                            // TODO
                        }
                    },
                )
            }.toImmutableList()
    }

    public fun updateStructure(current: RootHomeScreenStructure.MonthlyCategory) {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                year = current.year,
                month = current.month,
                categoryId = current.categoryId,
            )
        }
        viewModelScope.launch {
            fetchCategoryName()
        }
        viewModelScope.launch {
            fetchList()
        }
    }

    private suspend fun fetchCategoryName() {
        val categoryId = viewModelStateFlow.value.categoryId
        val query = MonthlyCategoryScreenQuery(id = categoryId)
        val response = withContext(Dispatchers.IO) {
            graphqlClient.apolloClient.query(query)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
        }

        if (!response.hasErrors()) {
            val categoryName = response.data?.user?.moneyUsageCategory?.name
            if (categoryName != null) {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        categoryName = categoryName,
                    )
                }
            }
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
                                moneyUsages = MonthlyCategoryScreenListQuery.MoneyUsages(
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

    private suspend fun queryExec(query: MonthlyCategoryScreenListQuery): ApolloResponse<MonthlyCategoryScreenListQuery.Data> {
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
        val categoryId: MoneyUsageCategoryId,
        val categoryName: String? = null,
        val apolloResponse: ApolloResponse<MonthlyCategoryScreenListQuery.Data>? = null,
    ) {
        fun createFirstQuery(): MonthlyCategoryScreenListQuery {
            return createQuery(
                cursor = null,
                categoryId = categoryId,
                year = year,
                month = month,
            )
        }

        fun createQuery(cursor: String?): MonthlyCategoryScreenListQuery {
            return createQuery(
                cursor = cursor,
                categoryId = categoryId,
                year = year,
                month = month,
            )
        }
    }
}

private fun createQuery(
    cursor: String?,
    categoryId: MoneyUsageCategoryId,
    year: Int,
    month: Int,
): MonthlyCategoryScreenListQuery {
    val date = LocalDate(
        year = year,
        monthNumber = month,
        dayOfMonth = 1,
    )
    return MonthlyCategoryScreenListQuery(
        cursor = Optional.present(cursor),
        size = 50,
        category = categoryId,
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
