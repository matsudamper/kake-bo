package net.matsudamper.money.frontend.common.viewmodel.root.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.RootUsageListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageUsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery

public class RootUsageListViewModel(
    private val coroutineScope: CoroutineScope,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val paging = ApolloPagingResponseCollector.create<UsageUsageListScreenPagingQuery.Data>(
        apolloClient = apolloClient,
        coroutineScope = coroutineScope,
    )

    public val uiStateFlow: StateFlow<RootUsageListScreenUiState> = MutableStateFlow(
        RootUsageListScreenUiState(
            loadingState = RootUsageListScreenUiState.LoadingState.Loading,
            event = object : RootUsageListScreenUiState.Event {
                override fun onViewInitialized() {
                    fetch()
                }

                override fun onClickAdd() {
                    coroutineScope.launch {
                        viewModelEventSender.send {
                            it.navigate(ScreenStructure.AddMoneyUsage())
                        }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow
                .collectLatest { viewModelState ->
                    val nodes = viewModelState.results.mapNotNull { state ->
                        state.getSuccessOrNull()?.value
                    }.flatMap {
                        it.data?.user?.moneyUsages?.nodes.orEmpty()
                    }
                    val items = buildList {
                        var lastMonth: LocalDateTime? = null
                        nodes.forEach { result ->
                            if (lastMonth == null || lastMonth?.month != result.date.month) {
                                add(
                                    RootUsageListScreenUiState.Item.Title(
                                        title = buildString {
                                            append("${result.date.year}年")
                                            append("${result.date.monthNumber}月")
                                        },
                                    ),
                                )
                                lastMonth = result.date
                            }
                            add(
                                RootUsageListScreenUiState.Item.Usage(
                                    title = result.title,
                                    amount = "${Formatter.formatMoney(result.amount)}円",
                                    date = Formatter.formatDateTime(result.date),
                                    category = run category@{
                                        val subCategory =
                                            result.moneyUsageSubCategory ?: return@category null

                                        "${subCategory.category.name} / ${subCategory.name}"
                                    },
                                    event = object : RootUsageListScreenUiState.ItemEvent {
                                        override fun onClick() {
                                            coroutineScope.launch {
                                                viewModelEventSender.send {
                                                    it.navigate(
                                                        ScreenStructure.MoneyUsage(
                                                            id = result.id,
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                    },
                                ),
                            )
                        }
                    }.toImmutableList()

                    val hasMore = viewModelState.results.lastOrNull()
                        ?.getSuccessOrNull()?.value?.data?.user?.moneyUsages?.hasMore
                        ?: true

                    uiStateFlow.update { uiState ->
                        uiState.copy(
                            loadingState = RootUsageListScreenUiState.LoadingState.Loaded(
                                loadToEnd = hasMore.not(),
                                items = items,
                                event = object : RootUsageListScreenUiState.LoadedEvent {
                                    override fun loadMore() {
                                        fetch()
                                    }
                                },
                            ),
                        )
                    }
                }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            paging.flow.collectLatest { responseStates ->
                viewModelStateFlow.update {
                    it.copy(
                        results = responseStates,
                    )
                }
            }
        }
    }

    private fun fetch() {
        paging.add { collectors ->
            val cursor: String?
            when (val lastState = collectors.lastOrNull()?.flow?.value) {
                is ApolloResponseState.Loading -> return@add null
                is ApolloResponseState.Failure -> {
                    paging.lastRetry()
                    return@add null
                }

                null -> {
                    cursor = null
                }

                is ApolloResponseState.Success -> {
                    val result = lastState.value.data?.user?.moneyUsages
                    if (result == null) {
                        paging.lastRetry()
                        return@add null
                    }
                    if (result.hasMore.not()) return@add null

                    cursor = result.cursor
                }
            }
            UsageUsageListScreenPagingQuery(
                query = MoneyUsagesQuery(
                    cursor = Optional.present(cursor),
                    size = 10,
                ),
            )
        }
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    private data class ViewModelState(
        val results: List<ApolloResponseState<ApolloResponse<UsageUsageListScreenPagingQuery.Data>>> = listOf(),
    )
}
