package net.matsudamper.money.frontend.common.viewmodel.root.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.feature.uploader.ImageUploadQueue
import net.matsudamper.money.frontend.common.ui.screen.root.settings.UploadQueueDebugScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel

public class UploadQueueDebugViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val imageUploadQueue: ImageUploadQueue,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelState = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<UploadQueueDebugScreenUiState> = MutableStateFlow(
        UploadQueueDebugScreenUiState(
            loadingState = UploadQueueDebugScreenUiState.LoadingState.Loading,
            event = object : UploadQueueDebugScreenUiState.Event {
                override fun onLoadMore() {
                    loadMore()
                }

                override fun onRetry() {
                    viewModelState.update {
                        it.copy(
                            items = listOf(),
                            offset = 0,
                            isLast = false,
                        )
                    }
                    loadMore()
                }
            },
        ),
    ).also { stateFlow ->
        viewModelScope.launch {
            viewModelState.collect { state ->
                stateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = if (state.isLoading && state.items.isEmpty()) {
                            UploadQueueDebugScreenUiState.LoadingState.Loading
                        } else {
                            UploadQueueDebugScreenUiState.LoadingState.Loaded(
                                items = state.items.map { item ->
                                    UploadQueueDebugScreenUiState.Item(
                                        id = item.id,
                                        moneyUsageId = item.moneyUsageId,
                                        status = when (val s = item.status) {
                                            ImageUploadQueue.Status.Pending -> UploadQueueDebugScreenUiState.Status.Pending
                                            ImageUploadQueue.Status.Uploading -> UploadQueueDebugScreenUiState.Status.Uploading
                                            is ImageUploadQueue.Status.Failed -> UploadQueueDebugScreenUiState.Status.Failed(s.message)
                                        },
                                        errorMessage = item.errorMessage,
                                        createdAt = item.createdAt,
                                        workManagerId = item.workManagerId,
                                    )
                                },
                                isLoadingMore = state.isLoading,
                                isLast = state.isLast,
                            )
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        loadMore()
    }

    private fun loadMore() {
        val currentState = viewModelState.value
        if (currentState.isLoading || currentState.isLast) return

        viewModelState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val newItems = runCatching {
                imageUploadQueue.getPagedDebugItems(
                    offset = currentState.offset,
                    limit = PAGE_SIZE,
                )
            }.getOrDefault(listOf())

            viewModelState.update { state ->
                state.copy(
                    items = state.items + newItems,
                    offset = state.offset + newItems.size,
                    isLast = newItems.size < PAGE_SIZE,
                    isLoading = false,
                )
            }
        }
    }

    private data class ViewModelState(
        val items: List<ImageUploadQueue.DebugItem> = listOf(),
        val offset: Int = 0,
        val isLast: Boolean = false,
        val isLoading: Boolean = false,
    )

    private companion object {
        const val PAGE_SIZE = 20
    }
}
