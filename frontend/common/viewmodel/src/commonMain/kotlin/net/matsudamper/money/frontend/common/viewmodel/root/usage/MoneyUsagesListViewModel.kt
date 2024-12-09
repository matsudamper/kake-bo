package net.matsudamper.money.frontend.common.viewmodel.root.usage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.usage.RootUsageListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.lib.Formatter
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState

public class MoneyUsagesListViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    graphqlClient: GraphqlClient,
    rootUsageHostViewModel: RootUsageHostViewModel,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val pagingModel = MoneyUsagesListFetchModel(
        graphqlClient = graphqlClient,
        coroutineScope = viewModelScope,
    )

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    public val uiStateFlow: StateFlow<RootUsageListScreenUiState> =
        MutableStateFlow(
            RootUsageListScreenUiState(
                loadingState = RootUsageListScreenUiState.LoadingState.Loading,
                hostScreenUiState = rootUsageHostViewModel.uiStateFlow.value,
                event =
                object : RootUsageListScreenUiState.Event {
                    override suspend fun onViewInitialized() {
                        CoroutineScope(currentCoroutineContext()).launch {
                            launch {
                                pagingModel.fetch()
                            }
                            launch {
                                rootUsageHostViewModel.viewModelStateFlow
                                    .buffer(Channel.RENDEZVOUS)
                                    .collectLatest {
                                        delay(100)
                                        pagingModel.changeText(it.searchText)
                                        pagingModel.fetch()
                                    }
                            }
                            launch {
                                pagingModel.getFlow().collectLatest { responseStates ->
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
            viewModelScope.launch {
                rootUsageHostViewModel.uiStateFlow
                    .collectLatest { hostUiState ->
                        uiStateFlow.update { uiState ->
                            uiState.copy(
                                hostScreenUiState = hostUiState,
                            )
                        }
                    }
            }
            viewModelScope.launch {
                viewModelStateFlow
                    .collectLatest { viewModelState ->
                        val nodes =
                            viewModelState.results.mapNotNull { state ->
                                state.getSuccessOrNull()?.value
                            }.flatMap {
                                it.data?.user?.moneyUsages?.nodes.orEmpty()
                            }
                        val items =
                            buildList {
                                var lastMonth: LocalDateTime? = null
                                nodes.forEach { result ->
                                    if (lastMonth == null || lastMonth?.month != result.date.month) {
                                        add(
                                            RootUsageListScreenUiState.Item.Title(
                                                title =
                                                buildString {
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
                                            category =
                                            run category@{
                                                val subCategory =
                                                    result.moneyUsageSubCategory ?: return@category null

                                                "${subCategory.category.name} / ${subCategory.name}"
                                            },
                                            event =
                                            object : RootUsageListScreenUiState.ItemEvent {
                                                override fun onClick() {
                                                    viewModelScope.launch {
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

                        val hasMore =
                            viewModelState.results.lastOrNull()
                                ?.getSuccessOrNull()?.value?.data?.user?.moneyUsages?.hasMore
                                ?: true

                        uiStateFlow.update { uiState ->
                            uiState.copy(
                                loadingState =
                                RootUsageListScreenUiState.LoadingState.Loaded(
                                    loadToEnd = hasMore.not(),
                                    items = items,
                                    event =
                                    object : RootUsageListScreenUiState.LoadedEvent {
                                        override fun loadMore() {
                                            viewModelScope.launch {
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
