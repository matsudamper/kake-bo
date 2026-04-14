package net.matsudamper.money.frontend.common.viewmodel.root.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val event = object : UploadQueueDebugScreenUiState.Event {
        override fun onClickStatusFilter() {
            viewModelState.update { it.copy(statusFilterExpanded = true) }
        }

        override fun onDismissStatusFilter() {
            viewModelState.update { it.copy(statusFilterExpanded = false) }
        }

        override fun onSelectStatusFilter(filter: UploadQueueDebugScreenUiState.StatusFilter) {
            viewModelState.update {
                it.copy(
                    selectedStatusFilter = filter,
                    statusFilterExpanded = false,
                )
            }
        }
    }

    public val uiStateFlow: StateFlow<UploadQueueDebugScreenUiState> = MutableStateFlow(
        UploadQueueDebugScreenUiState(
            items = listOf(),
            selectedStatusFilter = UploadQueueDebugScreenUiState.StatusFilter.All,
            statusFilterExpanded = false,
            event = event,
        ),
    ).also { stateFlow ->
        viewModelScope.launch {
            combine(
                imageUploadQueue.observeAllDebugItems(),
                viewModelState,
            ) { allItems, state ->
                val filteredItems = when (state.selectedStatusFilter) {
                    UploadQueueDebugScreenUiState.StatusFilter.All -> allItems
                    UploadQueueDebugScreenUiState.StatusFilter.Pending -> allItems.filter { it.status is ImageUploadQueue.Status.Pending }
                    UploadQueueDebugScreenUiState.StatusFilter.Uploading -> allItems.filter { it.status is ImageUploadQueue.Status.Uploading }
                    UploadQueueDebugScreenUiState.StatusFilter.Completed -> allItems.filter { it.status is ImageUploadQueue.Status.Completed }
                    UploadQueueDebugScreenUiState.StatusFilter.Failed -> allItems.filter { it.status is ImageUploadQueue.Status.Failed }
                }
                filteredItems to state
            }.collect { (filteredItems, state) ->
                stateFlow.update { uiState ->
                    uiState.copy(
                        items = filteredItems.map { item ->
                            UploadQueueDebugScreenUiState.Item(
                                id = item.id,
                                moneyUsageId = item.moneyUsageId,
                                status = when (val s = item.status) {
                                    ImageUploadQueue.Status.Pending -> UploadQueueDebugScreenUiState.Status.Pending
                                    ImageUploadQueue.Status.Uploading -> UploadQueueDebugScreenUiState.Status.Uploading
                                    ImageUploadQueue.Status.Completed -> UploadQueueDebugScreenUiState.Status.Completed
                                    is ImageUploadQueue.Status.Failed -> UploadQueueDebugScreenUiState.Status.Failed(s.message)
                                },
                                errorMessage = item.errorMessage,
                                createdAt = item.createdAt,
                                workManagerId = item.workManagerId,
                            )
                        },
                        selectedStatusFilter = state.selectedStatusFilter,
                        statusFilterExpanded = state.statusFilterExpanded,
                    )
                }
            }
        }
    }.asStateFlow()

    private data class ViewModelState(
        val selectedStatusFilter: UploadQueueDebugScreenUiState.StatusFilter = UploadQueueDebugScreenUiState.StatusFilter.All,
        val statusFilterExpanded: Boolean = false,
    )
}
