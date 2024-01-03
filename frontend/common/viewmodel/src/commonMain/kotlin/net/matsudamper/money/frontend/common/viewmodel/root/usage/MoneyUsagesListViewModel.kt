package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class MoneyUsagesListViewModel(
    private val coroutineScope: CoroutineScope,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
    rootUsageHostViewModel: RootUsageHostViewModel,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val pagingModel = MoneyUsagesListFetchModel(
        apolloClient = apolloClient,
        coroutineScope = coroutineScope,
    )

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    public val uiStateFlow: StateFlow<RootUsageListScreenUiState> = MutableStateFlow(
        RootUsageListScreenUiState(
            loadingState = RootUsageListScreenUiState.LoadingState.Loading,
            hostScreenUiState = rootUsageHostViewModel.uiStateFlow.value,
            event = object : RootUsageListScreenUiState.Event {
                override suspend fun onViewInitialized() {
                    coroutineScope {
                        launch {
                            pagingModel.fetch()
                        }
                        launch {
                            pagingModel.flow.collectLatest { responseStates ->
                                viewModelStateFlow.update {
                                    it.copy(
                                        results = responseStates,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            rootUsageHostViewModel.uiStateFlow
                .collectLatest { hostUiState ->
                    uiStateFlow.update { uiState ->
                        uiState.copy(
                            hostScreenUiState = hostUiState,
                        )
                    }
                }
        }
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
                                        coroutineScope.launch {
                                            pagingModel.fetch()
                                        }
                                    }
                                },
                            ),
                        )
                    }
                }
        }
    }.asStateFlow()

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    private data class ViewModelState(
        val results: List<ApolloResponseState<ApolloResponse<UsageListScreenPagingQuery.Data>>> = listOf(),
    )
}
