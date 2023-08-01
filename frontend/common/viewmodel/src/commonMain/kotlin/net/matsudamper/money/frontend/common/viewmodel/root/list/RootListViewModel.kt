package net.matsudamper.money.frontend.common.viewmodel.root.list

import com.apollographql.apollo3.api.ApolloResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.ui.screen.RootListScreenUiState
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
                override fun onClickAdd() {
                    coroutineScope.launch {
                        viewModelEventSender.send {
                            it.navigateToAddMoneyUsage()
                        }
                    }
                }

                override fun loadMore() {
                    fetch()
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                val results = flow {
                    viewModelState.results
                        .forEach { emitAll(it) }
                }.toList()

                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = RootListScreenUiState.LoadingState.Loaded(
                            loadToEnd = viewModelState.loadToEnd,
                            items = results.flatMap { it.data?.user?.moneyUsages?.nodes.orEmpty() }.map {
                                RootListScreenUiState.Item(
                                    title = it.title,
                                    amount = it.amount.toString(),
                                    date = it.date.toString(),
                                    description = it.description,
                                    category = run category@{
                                        val subCategory = it.moneyUsageSubCategory ?: return@category null

                                        "${subCategory.name} / ${subCategory.category.name}"
                                    },
                                )
                            },
                        )
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        fetch()
    }

    private fun fetch() {
        coroutineScope.launch {
            if (viewModelStateFlow.value.isLoading) return@launch
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    isLoading = true,
                )
            }
            val beforeResults = viewModelStateFlow.value.results.lastOrNull()
            val cursor = if (beforeResults == null) {
                null
            } else {
                val tmp = beforeResults.lastOrNull()
                tmp?.data?.user?.moneyUsages?.cursor
                    ?: return@launch
            }

            val result = api.getHomeScreen(
                cursor = cursor,
            )
            val loadToEnd = result.first().data?.user?.moneyUsages?.let { moneyUsages ->
                moneyUsages.cursor == null
            }
            if (loadToEnd == null) {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        isLoading = false,
                    )
                }
                return@launch
            }
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    results = viewModelState.results.plus(result),
                    isLoading = false,
                    loadToEnd = loadToEnd,
                )
            }
        }
    }

    public interface Event {
        public fun navigateToAddMoneyUsage()
    }

    private data class ViewModelState(
        val results: List<Flow<ApolloResponse<UsageListScreenPagingQuery.Data>>> = listOf(),
        val isLoading: Boolean = false,
        val loadToEnd: Boolean = false,
    )
}
