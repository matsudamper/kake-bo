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
import kotlinx.coroutines.flow.flow
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
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                val results = flow {
                    viewModelState.results
                        .forEach { emitAll(it) }
                }.toList()

                results.flatMap { it.data?.user?.moneyUsages?.nodes.orEmpty() }


                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = RootListScreenUiState.LoadingState.Loaded(
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
        coroutineScope.launch {
            val result = api.getHomeScreen()
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    results = viewModelState.results.plus(result),
                )
            }


//            api.getHomeScreen()
//                .catch { /* TODO */ }
//                .collect { response ->
////                    val moneyUsages = response.data?.user?.moneyUsages
////                    if (moneyUsages == null) {
////                        // TODO Error
////                        return@collect
////                    }
////                    viewModelStateFlow.update { viewState ->
////                        viewState.copy(
////                            listLoadingStatus = viewState.listLoadingStatus.copy(
////                                cursor = moneyUsages.cursor,
////                                nodes = viewState.listLoadingStatus.nodes + moneyUsages.nodes,
////                                finishLoading = moneyUsages.nodes.isEmpty(),
////                            ),
////                        )
////                    }
//                }
        }
    }

    private fun fetch() {

    }

    public interface Event {
        public fun navigateToAddMoneyUsage()
    }

    private data class ViewModelState(
//        val listLoadingStatus: ListLoadingStatus = ListLoadingStatus(),
        val results: List<Flow<ApolloResponse<UsageListScreenPagingQuery.Data>>> = listOf(),
    ) {
//        data class ListLoadingStatus(
//            val isLoading: Boolean = false,
//            val cursor: String? = null,
//            val nodes: List<UsageListScreenPagingQuery.Node> = emptyList(),
//            val finishLoading: Boolean = false,
//        )
    }
}
