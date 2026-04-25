package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.ApolloResponse
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminUnlinkedImagesScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.AdminUnlinkedImagesQuery
import net.matsudamper.money.frontend.graphql.AdminUnlinkedImagesTotalCountQuery
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UpdateOperationResponseResult

private const val TAG = "AdminUnlinkedImagesScreenViewModel"

public class AdminUnlinkedImagesScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    graphqlClient: GraphqlClient,
    private val adminQuery: GraphqlAdminQuery,
) : CommonViewModel(scopedObjectFeature) {
    private val pagingModel = AdminUnlinkedImagesPagingModel(
        graphqlClient = graphqlClient,
    )
    private val totalCountModel = AdminUnlinkedImagesTotalCountModel(
        graphqlClient = graphqlClient,
    )
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val event = object : AdminUnlinkedImagesScreenUiState.Event {
        override fun onResume() {
            if (viewModelStateFlow.value.hasLoaded.not()) {
                requestLoadInitialData(false)
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
            viewModelStateFlow.collectLatest { state ->
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
                    val connection = state.apolloResponseState?.data?.adminUnlinkedImages
                    val items = connection?.nodes.orEmpty()
                    uiState.copy(
                        loadingState = when {
                            connection == null && (state.isLoading || state.hasLoaded.not()) -> AdminUnlinkedImagesScreenUiState.LoadingState.Loading
                            connection == null -> AdminUnlinkedImagesScreenUiState.LoadingState.Error
                            else -> AdminUnlinkedImagesScreenUiState.LoadingState.Loaded(
                                items = items.map { item ->
                                    AdminUnlinkedImagesScreenUiState.Item(
                                        id = item.id.toString(),
                                        imageUrl = item.url,
                                        userId = item.userId.value.toString(),
                                        userName = item.userName,
                                        isSelected = item.id in state.selectedImageIds,
                                        event = createItemEvent(item.id),
                                    )
                                },
                                hasMore = connection.hasMore,
                                isLoadingMore = state.isLoadingMore,
                                totalCount = state.totalCountResponse?.data?.adminUnlinkedImages?.totalCount,
                                selectedCount = state.selectedImageIds.size,
                                isAllSelected = items.isNotEmpty() &&
                                    connection.hasMore.not() &&
                                    state.selectedImageIds.size == items.size &&
                                    items.all { it.id in state.selectedImageIds },
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

    init {
        viewModelScope.launch {
            pagingModel.getFlow().collectLatest { response ->
                val loadedIds = response.data?.adminUnlinkedImages?.nodes.orEmpty()
                    .map { it.id }
                    .toSet()
                viewModelStateFlow.update {
                    it.copy(
                        apolloResponseState = response,
                        selectedImageIds = it.selectedImageIds.filter { imageId ->
                            imageId in loadedIds
                        }.toSet(),
                    )
                }
            }
        }
        viewModelScope.launch {
            totalCountModel.getFlow().collectLatest { response ->
                viewModelStateFlow.update {
                    it.copy(
                        totalCountResponse = response,
                    )
                }
            }
        }
    }

    private fun requestLoadInitialData(force: Boolean) {
        viewModelScope.launch {
            refreshData(force = force)
        }
    }

    private suspend fun refreshData(force: Boolean) {
        if (viewModelStateFlow.value.isLoading) return

        viewModelStateFlow.update {
            it.copy(
                selectedImageIds = it.selectedImageIds.takeIf { !force }.orEmpty(),
                isLoading = true,
                isLoadingMore = false,
                isSelectingAll = false,
                deleteDialogState = null,
            )
        }

        when (val result = totalCountModel.refresh()) {
            is UpdateOperationResponseResult.Error -> {
                result.e?.let {
                    Logger.e(TAG, it)
                }
            }

            is UpdateOperationResponseResult.NoHasMore,
            is UpdateOperationResponseResult.Success,
            -> Unit
        }

        val result = runCatching {
            pagingModel.refresh()
        }.onFailure {
            Logger.e(TAG, it)
        }.getOrElse { UpdateOperationResponseResult.Error(it) }

        viewModelStateFlow.update {
            it.copy(
                lastLoadingState = result,
                isLoading = false,
                hasLoaded = true,
            )
        }
    }

    private fun fetchNextPage() {
        val state = viewModelStateFlow.value
        val connection = state.apolloResponseState?.data?.adminUnlinkedImages ?: return
        if (state.isLoadingMore || connection.hasMore.not() || state.isSelectingAll || state.isLoading) return
        if (connection.cursor == null) return

        viewModelScope.launch {
            viewModelStateFlow.update { it.copy(isLoadingMore = true) }
            val result = runCatching {
                pagingModel.fetch()
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrElse { UpdateOperationResponseResult.Error(it) }

            viewModelStateFlow.update {
                it.copy(
                    lastLoadingState = result,
                    isLoadingMore = false,
                )
            }
        }
    }

    private fun toggleSelectAll() {
        val state = viewModelStateFlow.value
        val connection = state.apolloResponseState?.data?.adminUnlinkedImages ?: return
        if (connection.nodes.isEmpty() || state.isSelectingAll || state.isLoading || state.deleteDialogState?.isLoading == true) {
            return
        }

        val isAllSelected = connection.hasMore.not() &&
            state.selectedImageIds.size == connection.nodes.size &&
            connection.nodes.all { it.id in state.selectedImageIds }
        if (isAllSelected) {
            viewModelStateFlow.update { it.copy(selectedImageIds = emptySet()) }
            return
        }

        viewModelScope.launch {
            viewModelStateFlow.update { it.copy(isSelectingAll = true) }

            var items = connection.nodes
            var hasMore = connection.hasMore

            while (hasMore) {
                when (
                    val result = runCatching {
                        pagingModel.fetch()
                    }.onFailure {
                        Logger.e(TAG, it)
                    }.getOrElse { UpdateOperationResponseResult.Error(it) }
                ) {
                    is UpdateOperationResponseResult.Error -> {
                        viewModelStateFlow.update {
                            it.copy(
                                lastLoadingState = result,
                                isSelectingAll = false,
                            )
                        }
                        return@launch
                    }

                    is UpdateOperationResponseResult.NoHasMore -> {
                        viewModelStateFlow.update {
                            it.copy(
                                lastLoadingState = result,
                            )
                        }
                        hasMore = false
                    }

                    is UpdateOperationResponseResult.Success -> {
                        val newConnection = result.result.data?.adminUnlinkedImages
                            ?: run {
                                viewModelStateFlow.update {
                                    it.copy(
                                        isSelectingAll = false,
                                    )
                                }
                                return@launch
                            }
                        items = newConnection.nodes
                        hasMore = newConnection.hasMore
                        viewModelStateFlow.update {
                            it.copy(
                                lastLoadingState = result,
                            )
                        }
                    }
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

            runCatching {
                pagingModel.removeDeletedImagesFromCache(imageIds)
                totalCountModel.removeDeletedImagesFromCache(imageIds)
            }.onFailure {
                Logger.e(TAG, it)
            }

            viewModelStateFlow.update {
                it.copy(
                    selectedImageIds = emptySet(),
                    deleteDialogState = null,
                )
            }
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

    private data class ViewModelState(
        val apolloResponseState: ApolloResponse<AdminUnlinkedImagesQuery.Data>? = null,
        val totalCountResponse: ApolloResponse<AdminUnlinkedImagesTotalCountQuery.Data>? = null,
        val lastLoadingState: UpdateOperationResponseResult<AdminUnlinkedImagesQuery.Data>? = null,
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasLoaded: Boolean = false,
        val selectedImageIds: Set<ImageId> = emptySet(),
        val isSelectingAll: Boolean = false,
        val deleteDialogState: DeleteDialogState? = null,
    )

    private data class DeleteDialogState(
        val isLoading: Boolean,
        val errorMessage: String?,
    )

    private companion object {
        private const val PAGE_SIZE = 60
    }
}
