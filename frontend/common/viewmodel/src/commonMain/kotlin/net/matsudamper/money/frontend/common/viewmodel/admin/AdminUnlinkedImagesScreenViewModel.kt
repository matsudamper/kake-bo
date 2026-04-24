package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminUnlinkedImagesScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.AdminUnlinkedImagesQuery
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

private const val TAG = "AdminUnlinkedImagesScreenViewModel"

public class AdminUnlinkedImagesScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val event = object : AdminUnlinkedImagesScreenUiState.Event {
        override fun onResume() {
            if (viewModelStateFlow.value.hasLoaded.not()) {
                fetchTotalCount()
                fetchFirstPage()
            }
        }

        override fun onClickRetry() {
            fetchTotalCount()
            fetchFirstPage()
        }

        override fun onClickLoadMore() {
            fetchNextPage()
        }
    }

    public val uiStateFlow: StateFlow<AdminUnlinkedImagesScreenUiState> = MutableStateFlow(
        AdminUnlinkedImagesScreenUiState(
            loadingState = AdminUnlinkedImagesScreenUiState.LoadingState.Loading,
            event = event,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { state ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = when {
                            state.isLoadingFirst -> AdminUnlinkedImagesScreenUiState.LoadingState.Loading
                            state.isError -> AdminUnlinkedImagesScreenUiState.LoadingState.Error
                            else -> AdminUnlinkedImagesScreenUiState.LoadingState.Loaded(
                                items = state.items.map { item ->
                                    AdminUnlinkedImagesScreenUiState.Item(
                                        id = item.id.toString(),
                                        imageUrl = item.url,
                                        userId = item.userId.value.toString(),
                                        userName = item.userName,
                                    )
                                },
                                hasMore = state.hasMore,
                                isLoadingMore = state.isLoadingMore,
                                totalCount = state.totalCount,
                            )
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private fun fetchTotalCount() {
        viewModelScope.launch {
            val totalCount = runCatching {
                adminQuery.getUnlinkedImagesTotalCount()
                    .data?.adminUnlinkedImages?.totalCount
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()

            if (totalCount != null) {
                viewModelStateFlow.update { it.copy(totalCount = totalCount) }
            }
        }
    }

    private fun fetchFirstPage() {
        viewModelScope.launch {
            viewModelStateFlow.update {
                it.copy(
                    isLoadingFirst = true,
                    isLoadingMore = false,
                    isError = false,
                )
            }
            val connection = runCatching {
                adminQuery.getUnlinkedImages(
                    size = PAGE_SIZE,
                    cursor = null,
                ).data?.adminUnlinkedImages
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()

            if (connection == null) {
                viewModelStateFlow.update {
                    it.copy(
                        isLoadingFirst = false,
                        isError = true,
                        hasLoaded = true,
                    )
                }
                return@launch
            }

            viewModelStateFlow.update {
                it.copy(
                    items = connection.nodes,
                    cursor = connection.cursor,
                    hasMore = connection.hasMore,
                    isLoadingFirst = false,
                    isError = false,
                    hasLoaded = true,
                )
            }
        }
    }

    private fun fetchNextPage() {
        val state = viewModelStateFlow.value
        if (state.isLoadingMore || state.hasMore.not()) return
        val cursor = state.cursor ?: return

        viewModelScope.launch {
            viewModelStateFlow.update { it.copy(isLoadingMore = true) }
            val connection = runCatching {
                adminQuery.getUnlinkedImages(
                    size = PAGE_SIZE,
                    cursor = cursor,
                ).data?.adminUnlinkedImages
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()

            if (connection == null) {
                viewModelStateFlow.update { it.copy(isLoadingMore = false) }
                return@launch
            }

            viewModelStateFlow.update {
                it.copy(
                    items = it.items + connection.nodes,
                    cursor = connection.cursor,
                    hasMore = connection.hasMore,
                    isLoadingMore = false,
                )
            }
        }
    }

    private data class ViewModelState(
        val items: List<AdminUnlinkedImagesQuery.Node> = listOf(),
        val cursor: String? = null,
        val hasMore: Boolean = false,
        val isLoadingFirst: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isError: Boolean = false,
        val hasLoaded: Boolean = false,
        val totalCount: Int? = null,
    )

    private companion object {
        private const val PAGE_SIZE = 60
    }
}
