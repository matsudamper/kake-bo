package net.matsudamper.money.frontend.common.viewmodel.root.list

import com.apollographql.apollo3.ApolloClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.RootListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery
import net.matsudamper.money.frontend.graphql.lib.ApolloPagingResponseCollector
import net.matsudamper.money.frontend.graphql.lib.ApolloResponseState
import net.matsudamper.money.frontend.graphql.type.MoneyUsagesQuery

public class RootListViewModel(
    private val coroutineScope: CoroutineScope,
    apolloClient: ApolloClient = GraphqlClient.apolloClient,
    private val api: HomeUsageListGraphqlApi,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val paging = ApolloPagingResponseCollector.create<UsageListScreenPagingQuery.Data>(
        apolloClient = apolloClient,
        coroutineScope = coroutineScope,
    )

    public val uiStateFlow: StateFlow<RootListScreenUiState> = MutableStateFlow(
        RootListScreenUiState(
            loadingState = RootListScreenUiState.LoadingState.Loading,
            event = object : RootListScreenUiState.Event {
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
                    val items = viewModelState.results.mapNotNull { state ->
                        state.getSuccessOrNull()?.value
                    }.flatMap {
                        it.data?.user?.moneyUsages?.nodes.orEmpty()
                    }.map { result ->
                        RootListScreenUiState.Item(
                            title = result.title,
                            amount = result.amount.toString(),
                            date = result.date.toString(),
                            description = result.description,
                            category = run category@{
                                val subCategory =
                                    result.moneyUsageSubCategory ?: return@category null

                                "${subCategory.name} / ${subCategory.category.name}"
                            },
                            event = object : RootListScreenUiState.ItemEvent {
                                override fun onClick() {
                                    coroutineScope.launch {
                                        viewModelEventSender.send {
                                            it.navigate(
                                                ScreenStructure.MoneyUsage(
                                                    id = result.id,
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }.toImmutableList()
                    val hasMore = viewModelState.results.lastOrNull()
                        ?.getSuccessOrNull()?.value?.data?.user?.moneyUsages?.hasMore
                        ?: true

                    uiStateFlow.update { uiState ->
                        uiState.copy(
                            loadingState = RootListScreenUiState.LoadingState.Loaded(
                                loadToEnd = hasMore.not(),
                                items = items,
                                event = object : RootListScreenUiState.LoadedEvent {
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
        val cursor: String?
        when (val lastState = paging.flow.value.lastOrNull()) {
            is ApolloResponseState.Loading -> return
            is ApolloResponseState.Failure -> {
                paging.lastRetry()
                return
            }

            null -> {
                cursor = null
            }

            is ApolloResponseState.Success -> {
                val result = lastState.value.data?.user?.moneyUsages ?: return paging.lastRetry()
                if (result.hasMore.not()) return

                cursor = result.cursor
            }
        }
        println("cursor: $cursor")
        paging.add(
            UsageListScreenPagingQuery(
                query = MoneyUsagesQuery(
                    cursor = Optional.present(cursor),
                    size = 10,
                ),
            ),
        )
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    private data class ViewModelState(
        val results: List<ApolloResponseState<ApolloResponse<UsageListScreenPagingQuery.Data>>> = listOf(),
    )
}
