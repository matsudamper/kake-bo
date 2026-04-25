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
                fetchMonths()
            }
        }

        override fun onClickRetry() {
            val state = viewModelStateFlow.value
            if (state.selectedYearMonth != null) {
                fetchMonthDetail(state.selectedYearMonth)
            } else {
                fetchMonths()
            }
        }

        override fun onClickMonth(yearMonth: String) {
            fetchMonthDetail(yearMonth)
        }

        override fun onClickBack() {
            viewModelStateFlow.update {
                it.copy(selectedYearMonth = null, monthDetailItems = listOf(), selectedIds = setOf())
            }
        }

        override fun onToggleImageSelection(id: String) {
            viewModelStateFlow.update { state ->
                val newSelectedIds = if (state.selectedIds.contains(id)) {
                    state.selectedIds - id
                } else {
                    state.selectedIds + id
                }
                state.copy(selectedIds = newSelectedIds)
            }
        }

        override fun onClickSelectAll() {
            viewModelStateFlow.update { state ->
                state.copy(selectedIds = state.monthDetailItems.map { it.id }.toSet())
            }
        }

        override fun onClickDeselectAll() {
            viewModelStateFlow.update { state ->
                state.copy(selectedIds = setOf())
            }
        }

        override fun onClickDeleteSelected() {
            deleteSelectedImages()
        }
    }

    public val uiStateFlow: StateFlow<AdminUnlinkedImagesScreenUiState> = MutableStateFlow(
        AdminUnlinkedImagesScreenUiState(
            screenState = AdminUnlinkedImagesScreenUiState.ScreenState.Loading,
            event = event,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { state ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        screenState = when {
                            state.isLoading -> AdminUnlinkedImagesScreenUiState.ScreenState.Loading
                            state.isError -> AdminUnlinkedImagesScreenUiState.ScreenState.Error
                            state.selectedYearMonth != null -> AdminUnlinkedImagesScreenUiState.ScreenState.MonthDetail(
                                yearMonth = state.selectedYearMonth,
                                items = state.monthDetailItems.map { item ->
                                    AdminUnlinkedImagesScreenUiState.Item(
                                        id = item.id,
                                        imageUrl = item.url,
                                        userId = item.userId,
                                        userName = item.userName,
                                    )
                                },
                                selectedIds = state.selectedIds,
                                isDeleting = state.isDeleting,
                            )
                            else -> AdminUnlinkedImagesScreenUiState.ScreenState.MonthList(
                                months = state.months.map { month ->
                                    AdminUnlinkedImagesScreenUiState.MonthItem(
                                        yearMonth = month.yearMonth,
                                        count = month.count,
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    private fun fetchMonths() {
        viewModelScope.launch {
            viewModelStateFlow.update { it.copy(isLoading = true, isError = false) }
            val months = runCatching {
                adminQuery.getImageDirectoryMonths()
                    .data?.adminImageDirectoryMonths
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()

            if (months == null) {
                viewModelStateFlow.update { it.copy(isLoading = false, isError = true, hasLoaded = true) }
                return@launch
            }

            viewModelStateFlow.update {
                it.copy(
                    months = months.map { m -> MonthData(yearMonth = m.yearMonth, count = m.count) },
                    isLoading = false,
                    isError = false,
                    hasLoaded = true,
                )
            }
        }
    }

    private fun fetchMonthDetail(yearMonth: String) {
        viewModelScope.launch {
            viewModelStateFlow.update {
                it.copy(selectedYearMonth = yearMonth, isLoading = true, isError = false, selectedIds = setOf())
            }
            val items = runCatching {
                adminQuery.getUnlinkedImagesByMonth(yearMonth)
                    .data?.adminUnlinkedImagesByMonth
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()

            if (items == null) {
                viewModelStateFlow.update { it.copy(isLoading = false, isError = true) }
                return@launch
            }

            viewModelStateFlow.update {
                it.copy(
                    monthDetailItems = items.map { item ->
                        ImageItem(
                            id = item.id.toString(),
                            imageId = item.id,
                            url = item.url,
                            userId = item.userId.value.toString(),
                            userName = item.userName,
                        )
                    },
                    isLoading = false,
                    isError = false,
                )
            }
        }
    }

    private fun deleteSelectedImages() {
        val state = viewModelStateFlow.value
        if (state.isDeleting || state.selectedIds.isEmpty()) return
        val yearMonth = state.selectedYearMonth ?: return

        val imageIds = state.monthDetailItems
            .filter { state.selectedIds.contains(it.id) }
            .map { it.imageId }

        viewModelScope.launch {
            viewModelStateFlow.update { it.copy(isDeleting = true) }
            val success = runCatching {
                adminQuery.deleteUnlinkedImages(imageIds)
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrDefault(false)

            if (success) {
                fetchMonthDetail(yearMonth)
            }
            viewModelStateFlow.update { it.copy(isDeleting = false) }
        }
    }

    private data class MonthData(
        val yearMonth: String,
        val count: Int,
    )

    private data class ImageItem(
        val id: String,
        val imageId: ImageId,
        val url: String,
        val userId: String,
        val userName: String,
    )

    private data class ViewModelState(
        val months: List<MonthData> = listOf(),
        val selectedYearMonth: String? = null,
        val monthDetailItems: List<ImageItem> = listOf(),
        val selectedIds: Set<String> = setOf(),
        val isLoading: Boolean = false,
        val isDeleting: Boolean = false,
        val isError: Boolean = false,
        val hasLoaded: Boolean = false,
    )
}
