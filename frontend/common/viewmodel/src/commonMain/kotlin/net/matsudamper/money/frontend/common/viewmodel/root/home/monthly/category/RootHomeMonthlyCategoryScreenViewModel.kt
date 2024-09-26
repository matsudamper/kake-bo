package net.matsudamper.money.frontend.common.viewmodel.root.home.monthly.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.home.RootHomeMonthlyCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.MonthlyCategoryScreenListQuery
import net.matsudamper.money.frontend.graphql.MonthlyCategoryScreenQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class RootHomeMonthlyCategoryScreenViewModel(
    private val coroutineScope: CoroutineScope,
    argument: RootHomeScreenStructure.MonthlyCategory,
    loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    private val graphqlClient: GraphqlClient,
) {
    private val viewModelStateFlow =
        MutableStateFlow(
            ViewModelState(
                year = argument.year,
                month = argument.month,
                categoryId = argument.categoryId,
            ),
        )
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val monthlyCategoryResultState: ApolloPagingResponseCollector<MonthlyCategoryScreenListQuery.Data> =
        ApolloPagingResponseCollector.create(
            graphqlClient = graphqlClient,
            fetchPolicy = FetchPolicy.CacheFirst,
            coroutineScope = coroutineScope,
        )
    private val loadedEvent =
        object : RootHomeMonthlyCategoryScreenUiState.LoadedEvent {
            override fun loadMore() {
                coroutineScope.launch {
                    fetch()
                }
            }
        }

    public val uiStateFlow: StateFlow<RootHomeMonthlyCategoryScreenUiState> =
        MutableStateFlow(
            RootHomeMonthlyCategoryScreenUiState(
                event =
                object : RootHomeMonthlyCategoryScreenUiState.Event {
                    override fun onViewInitialized() {
                        coroutineScope.launch {
                            loginCheckUseCase.check()
                        }
                        coroutineScope.launch {
                            monthlyCategoryResultState.getFlow().collectLatest { results ->
                                viewModelStateFlow.update { viewModelState ->
                                    viewModelState.copy(
                                        apolloResponses = results,
                                    )
                                }
                            }
                        }
                        coroutineScope.launch {
                            viewModelStateFlow.map { viewModelState ->
                                viewModelState.categoryId
                            }.stateIn(this).collectLatest { categoryId ->
                                val collector =
                                    ApolloResponseCollector.create(
                                        apolloClient = graphqlClient.apolloClient,
                                        fetchPolicy = FetchPolicy.CacheFirst,
                                        query =
                                        MonthlyCategoryScreenQuery(
                                            id = categoryId,
                                        ),
                                    )
                                collector.fetch()
                                collector.getFlow().collectLatest { responseState ->
                                    val categoryName = responseState.getSuccessOrNull()?.value?.data?.user?.moneyUsageCategory?.name
                                    viewModelStateFlow.update { viewModelState ->
                                        viewModelState.copy(
                                            categoryName = categoryName,
                                        )
                                    }
                                }
                            }
                        }
                        coroutineScope.launch {
                            fetch()
                        }
                    }
                },
                loadingState = RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading,
                title = "",
            ),
        ).also { uiStateFlow ->
            coroutineScope.launch {
                viewModelStateFlow.collectLatest { viewModelState ->
                    val state =
                        when (viewModelState.apolloResponses.firstOrNull()) {
                            is ApolloResponseState.Failure -> {
                                RootHomeMonthlyCategoryScreenUiState.LoadingState.Error
                            }

                            null,
                            is ApolloResponseState.Loading,
                            -> {
                                RootHomeMonthlyCategoryScreenUiState.LoadingState.Loading
                            }

                            is ApolloResponseState.Success -> {
                                RootHomeMonthlyCategoryScreenUiState.LoadingState.Loaded(
                                    items =
                                    viewModelState.apolloResponses.flatMap {
                                        it.getSuccessOrNull()?.value?.data?.user?.moneyUsages?.nodes.orEmpty()
                                    }.map { node ->
                                        createItem(node)
                                    },
                                    event = loadedEvent,
                                    hasMoreItem =
                                    viewModelState.apolloResponses.lastOrNull()
                                        ?.getSuccessOrNull()?.value
                                        ?.data?.user?.moneyUsages?.hasMore != false,
                                )
                            }
                        }
                    uiStateFlow.value =
                        uiStateFlow.value.copy(
                            loadingState = state,
                            title =
                            run {
                                val yearText = "${viewModelState.year}年${viewModelState.month}月"
                                val descriptionText =
                                    if (viewModelState.categoryName == null) {
                                        "カテゴリ別一覧"
                                    } else {
                                        "${viewModelState.categoryName}"
                                    }
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
            event =
            object : RootHomeMonthlyCategoryScreenUiState.Item.Event {
                override fun onClick() {
                    coroutineScope.launch {
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

    public fun updateStructure(current: RootHomeScreenStructure.MonthlyCategory) {
        viewModelStateFlow.update { viewModelState ->
            viewModelState.copy(
                year = current.year,
                month = current.month,
                categoryId = current.categoryId,
            )
        }
        coroutineScope.launch {
            fetch()
        }
    }

    private fun fetch() {
        monthlyCategoryResultState.add { results ->
            val cursor: String? =
                when (val lastResponseState = viewModelStateFlow.value.apolloResponses.lastOrNull()) {
                    null -> null

                    is ApolloResponseState.Success -> {
                        val moneyUsage = lastResponseState.value.data?.user?.moneyUsages ?: return@add null
                        if (moneyUsage.hasMore) {
                            moneyUsage.cursor
                        } else {
                            null
                        }
                    }

                    is ApolloResponseState.Failure,
                    is ApolloResponseState.Loading,
                    -> return@add null
                }
            val date =
                LocalDate(
                    year = viewModelStateFlow.value.year,
                    monthNumber = viewModelStateFlow.value.month,
                    dayOfMonth = 1,
                )
            MonthlyCategoryScreenListQuery(
                cursor = Optional.present(cursor),
                size = 50,
                category = viewModelStateFlow.value.categoryId,
                sinceDateTime =
                Optional.present(
                    LocalDateTime(
                        date = date,
                        time = LocalTime(0, 0),
                    ),
                ),
                untilDateTime =
                Optional.present(
                    LocalDateTime(
                        date = date.plus(1, DateTimeUnit.MONTH),
                        time = LocalTime(0, 0),
                    ),
                ),
            )
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
        val apolloResponses: List<ApolloResponseState<ApolloResponse<MonthlyCategoryScreenListQuery.Data>>> = listOf(),
    )
}
