package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.element.ImageId
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
                requestLoadInitialData()
            }
        }

        override fun onClickRetry() {
            requestLoadInitialData(force = true)
        }

        override fun onClickLoadMore() {
            fetchNextPage()
        }

        override fun onClickSelectAll() {
            toggleSelectAll()
        }

        override fun onClickDelete() {
            val selectedCount = viewModelStateFlow.value.selectedImageIds.size
            if (selectedCount <= 0) return

            viewModelStateFlow.update {
                it.copy(
                    deleteDialogState = DeleteDialogState(
                        isLoading = false,
                        errorMessage = null,
                    ),
                )
            }
        }
    }

    public val uiStateFlow: StateFlow<AdminUnlinkedImagesScreenUiState> = MutableStateFlow(
        AdminUnlinkedImagesScreenUiState(
            loadingState = AdminUnlinkedImagesScreenUiState.LoadingState.Loading,
            deleteDialog = null,
            event = event,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { state ->
                val deleteDialog = state.deleteDialogState?.let { deleteDialogState ->
                    AdminUnlinkedImagesScreenUiState.DeleteDialog(
                        selectedCount = state.selectedImageIds.size,
                        errorMessage = deleteDialogState.errorMessage,
                        isLoading = deleteDialogState.isLoading,
                        event = object : AdminUnlinkedImagesScreenUiState.DeleteDialog.Event {
                            override fun onConfirm() {
                                confirmDelete()
                            }

                            override fun onCancel() {
                                closeDeleteDialog()
                            }

                            override fun onDismiss() {
                                closeDeleteDialog()
                            }
                        },
                    )
                }
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
                                        isSelected = item.id in state.selectedImageIds,
                                        event = createItemEvent(item.id),
                                    )
                                },
                                hasMore = state.hasMore,
                                isLoadingMore = state.isLoadingMore,
                                totalCount = state.totalCount,
                                selectedCount = state.selectedImageIds.size,
                                isAllSelected = state.items.isNotEmpty() &&
                                    state.hasMore.not() &&
                                    state.selectedImageIds.size == state.items.size &&
                                    state.items.all { it.id in state.selectedImageIds },
                                isSelectingAll = state.isSelectingAll,
                                isDeleting = state.deleteDialogState?.isLoading == true,
                            )
                        },
                        deleteDialog = deleteDialog,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun requestLoadInitialData(force: Boolean = false) {
        viewModelScope.launch {
            loadInitialData(force = force)
        }
    }

    private suspend fun loadInitialData(force: Boolean = false) {
        val currentState = viewModelStateFlow.value
        if (currentState.isLoadingFirst && force.not()) return

        viewModelStateFlow.update {
            it.copy(
                items = if (force) {
                    listOf()
                } else {
                    it.items
                },
                cursor = if (force) null else it.cursor,
                hasMore = if (force) false else it.hasMore,
                selectedImageIds = if (force) {
                    emptySet()
                } else {
                    it.selectedImageIds
                },
                isLoadingFirst = true,
                isLoadingMore = false,
                isError = false,
                isSelectingAll = false,
                deleteDialogState = null,
                totalCount = if (force) null else it.totalCount,
            )
        }

        val totalCount = loadTotalCount()
        if (totalCount != null) {
            viewModelStateFlow.update { it.copy(totalCount = totalCount) }
        }

        val connection = loadPage(cursor = null)
        if (connection == null) {
            viewModelStateFlow.update {
                it.copy(
                    isLoadingFirst = false,
                    isError = true,
                    hasLoaded = true,
                )
            }
            return
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

    private fun fetchNextPage() {
        val state = viewModelStateFlow.value
        if (state.isLoadingMore || state.hasMore.not() || state.isSelectingAll || state.isLoadingFirst) return
        val cursor = state.cursor ?: return

        viewModelScope.launch {
            viewModelStateFlow.update { it.copy(isLoadingMore = true) }
            val connection = loadPage(cursor = cursor)

            if (connection == null) {
                viewModelStateFlow.update {
                    it.copy(
                        isLoadingMore = false,
                    )
                }
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

    private fun toggleSelectAll() {
        val state = viewModelStateFlow.value
        if (state.items.isEmpty() || state.isSelectingAll || state.isLoadingFirst || state.deleteDialogState?.isLoading == true) {
            return
        }

        val isAllSelected = state.hasMore.not() &&
            state.selectedImageIds.size == state.items.size &&
            state.items.all { it.id in state.selectedImageIds }
        if (isAllSelected) {
            viewModelStateFlow.update { it.copy(selectedImageIds = emptySet()) }
            return
        }

        viewModelScope.launch {
            viewModelStateFlow.update { it.copy(isSelectingAll = true) }

            var items = viewModelStateFlow.value.items
            var cursor = viewModelStateFlow.value.cursor
            var hasMore = viewModelStateFlow.value.hasMore

            while (hasMore) {
                val connection = loadPage(cursor = cursor)

                if (connection == null) {
                    viewModelStateFlow.update { it.copy(isSelectingAll = false) }
                    return@launch
                }

                items = items + connection.nodes
                cursor = connection.cursor
                hasMore = connection.hasMore

                viewModelStateFlow.update {
                    it.copy(
                        items = items,
                        cursor = cursor,
                        hasMore = hasMore,
                    )
                }
            }

            viewModelStateFlow.update {
                it.copy(
                    selectedImageIds = items.map { item -> item.id }.toSet(),
                    isSelectingAll = false,
                )
            }
        }
    }

    private fun confirmDelete() {
        val state = viewModelStateFlow.value
        val imageIds = state.selectedImageIds.toList()
        if (imageIds.isEmpty() || state.deleteDialogState?.isLoading == true) return

        viewModelScope.launch {
            viewModelStateFlow.update {
                it.copy(
                    deleteDialogState = it.deleteDialogState?.copy(
                        isLoading = true,
                        errorMessage = null,
                    ),
                )
            }

            val isSuccess = runCatching {
                adminQuery.deleteImages(imageIds)
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrDefault(false)

            if (isSuccess.not()) {
                viewModelStateFlow.update {
                    it.copy(
                        deleteDialogState = it.deleteDialogState?.copy(
                            isLoading = false,
                            errorMessage = "画像の削除に失敗しました",
                        ),
                    )
                }
                return@launch
            }

            viewModelStateFlow.update {
                it.copy(
                    selectedImageIds = emptySet(),
                    deleteDialogState = null,
                )
            }
            loadInitialData(force = true)
        }
    }

    private fun closeDeleteDialog() {
        val state = viewModelStateFlow.value
        if (state.deleteDialogState?.isLoading == true) return
        viewModelStateFlow.update {
            it.copy(deleteDialogState = null)
        }
    }

    private fun createItemEvent(imageId: ImageId): AdminUnlinkedImagesScreenUiState.Item.Event {
        return object : AdminUnlinkedImagesScreenUiState.Item.Event {
            override fun onClickSelect() {
                viewModelStateFlow.update { state ->
                    state.copy(
                        selectedImageIds = if (imageId in state.selectedImageIds) {
                            state.selectedImageIds - imageId
                        } else {
                            state.selectedImageIds + imageId
                        },
                    )
                }
            }
        }
    }

    private suspend fun loadTotalCount(): Int? {
        return runCatching {
            adminQuery.getUnlinkedImagesTotalCount()
                .data?.adminUnlinkedImages?.totalCount
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
    }

    private suspend fun loadPage(
        cursor: String?,
    ): PageData? {
        return runCatching {
            val connection = adminQuery.getUnlinkedImages(
                size = PAGE_SIZE,
                cursor = cursor,
            ).data?.adminUnlinkedImages
                ?: return@runCatching null
            PageData(
                nodes = connection.nodes,
                cursor = connection.cursor,
                hasMore = connection.hasMore,
            )
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrNull()
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
        val selectedImageIds: Set<ImageId> = emptySet(),
        val isSelectingAll: Boolean = false,
        val deleteDialogState: DeleteDialogState? = null,
    )

    private data class DeleteDialogState(
        val isLoading: Boolean,
        val errorMessage: String?,
    )

    private data class PageData(
        val nodes: List<AdminUnlinkedImagesQuery.Node>,
        val cursor: String?,
        val hasMore: Boolean,
    )

    private companion object {
        private const val PAGE_SIZE = 60
    }
}
