package net.matsudamper.money.frontend.common.viewmodel.root.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.RootListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.UsageListScreenPagingQuery

public class RootListViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: HomeUsageListGraphqlApi,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

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
                    combine(viewModelState.results) {
                        it
                    }.collect { results ->
                        uiStateFlow.update { uiState ->
                            uiState.copy(
                                loadingState = RootListScreenUiState.LoadingState.Loaded(
                                    loadToEnd = viewModelState.loadToEnd,
                                    items = results.flatMap { it.data?.user?.moneyUsages?.nodes.orEmpty() }
                                        .map { result ->
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
                                        },
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
        }
    }.asStateFlow()

    private fun fetch() {
        coroutineScope.launch {
            viewModelStateFlow.update { viewModelState ->
                if (viewModelStateFlow.value.isLoading) return@launch
                viewModelState.copy(
                    isLoading = true,
                )
            }
            val beforeResults = viewModelStateFlow.value.results.lastOrNull()
            val cursor = if (beforeResults == null) {
                null
            } else {
                val tmp = beforeResults.firstOrNull()
                tmp?.data?.user?.moneyUsages?.cursor
                    ?: return@launch
            }

            val result = api.getHomeScreen(
                cursor = cursor,
            ).stateIn(coroutineScope, SharingStarted.Eagerly, null)
                .filterNotNull()

            val loadToEnd = result.first().data?.user?.moneyUsages?.let { moneyUsages ->
                moneyUsages.cursor == null
            }
            try {
                if (loadToEnd != null) {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            results = viewModelState.results.plus(result),
                            loadToEnd = loadToEnd,
                        )
                    }
                }
            } finally {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        isLoading = false,
                    )
                }
            }
        }
    }

    public interface Event {
        public fun navigate(screenStructure: ScreenStructure)
    }

    private data class ViewModelState(
        val results: List<Flow<ApolloResponse<UsageListScreenPagingQuery.Data>>> = listOf(),
        val isLoading: Boolean = false,
        val loadToEnd: Boolean = false,
    )
}
