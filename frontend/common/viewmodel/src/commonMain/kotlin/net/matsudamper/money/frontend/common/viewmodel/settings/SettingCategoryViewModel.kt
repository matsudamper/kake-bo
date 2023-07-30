package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.settings.SettingCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.CategorySettingScreenQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenSubCategoriesPagingQuery

public class SettingCategoryViewModel(
    private val categoryId: MoneyUsageCategoryId,
    private val coroutineScope: CoroutineScope,
    private val api: SettingCategoryApi,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            responseList = listOf(),
            isFirstLoading = true,
        ),
    )

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val globalEventSender = EventSender<GlobalEvent>()
    public val globalEventHandler: EventHandler<GlobalEvent> = globalEventSender.asHandler()

    public val uiState: StateFlow<SettingCategoryScreenUiState> = MutableStateFlow(
        SettingCategoryScreenUiState(
            event = object : SettingCategoryScreenUiState.Event {
                override suspend fun onResume() {
                }

                override fun dismissCategoryInput() {
                    coroutineScope.launch {
                        viewModelStateFlow.update {
                            it.copy(
                                showAddSubCategoryNameInput = false,
                            )
                        }
                    }
                }

                override fun onClickAddSubCategoryButton() {
                    coroutineScope.launch {
                        viewModelStateFlow.update {
                            it.copy(
                                showAddSubCategoryNameInput = true,
                            )
                        }
                    }
                }

                override fun subCategoryNameInputCompleted(text: String) {
                    coroutineScope.launch {
                        val result = api.addSubCategory(
                            categoryId = categoryId,
                            name = text,
                        )?.data?.userMutation?.addSubCategory?.subCategory

                        if (result == null) {
                            launch {
                                globalEventSender.send {
                                    it.showNativeNotification("追加に失敗しました")
                                }
                            }
                            return@launch
                        } else {
                            launch {
                                globalEventSender.send {
                                    it.showSnackBar("${result.name}を追加しました")
                                }
                            }
                        }

                        viewModelStateFlow.update {
                            it.copy(
                                showAddSubCategoryNameInput = false,
                            )
                        }

                        initialFetchSubCategories()
                    }
                }

                override fun onClickChangeCategoryName() {
                    val categoryInfo = viewModelStateFlow.value.categoryInfo ?: return
                    coroutineScope.launch {
                        viewModelStateFlow.update {
                            it.copy(
                                showCategoryNameChangeInput = SettingCategoryScreenUiState.FullScreenInputDialog(
                                    initText = categoryInfo.name,
                                    event = categoryNameChangeUiEvent,
                                ),
                            )
                        }
                    }
                }
            },
            loadingState = SettingCategoryScreenUiState.LoadingState.Loading,
            showCategoryNameInput = false,
            showCategoryNameChangeDialog = null,
            categoryName = "",
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    val loadingState = if (viewModelState.isFirstLoading) {
                        SettingCategoryScreenUiState.LoadingState.Loading
                    } else {
                        val items = viewModelState.responseList.map {
                            it.nodes
                        }.flatten()
                        SettingCategoryScreenUiState.LoadingState.Loaded(
                            item = items.map { item ->
                                createItemUiState(item)
                            }.toImmutableList(),
                        )
                    }

                    uiState.copy(
                        loadingState = loadingState,
                        showCategoryNameInput = viewModelState.showAddSubCategoryNameInput,
                        showCategoryNameChangeDialog = viewModelState.showCategoryNameChangeInput,
                        categoryName = viewModelState.categoryInfo?.name.orEmpty(),
                    )
                }
            }
        }
    }.asStateFlow()

    private val categoryNameChangeUiEvent = object : SettingCategoryScreenUiState.FullScreenInputDialog.Event {
        override fun onDismiss() {
            dismiss()
        }

        override fun onTextInputCompleted(text: String) {
            coroutineScope.launch {
                val result = api.updateCategory(
                    id = categoryId,
                    name = text,
                )?.data?.userMutation?.updateCategory
                if (result == null) {
                    launch {
                        globalEventSender.send {
                            it.showNativeNotification("カテゴリ名の変更に失敗しました")
                        }
                    }
                } else {
                    launch {
                        globalEventSender.send {
                            it.showSnackBar("カテゴリ名を変更しました")
                        }
                    }
                }
                dismiss()
            }
        }

        private fun dismiss() {
            viewModelStateFlow.update {
                it.copy(
                    showCategoryNameChangeInput = null,
                )
            }
        }
    }

    init {
        initialFetchSubCategories()
        fetchCategoryInfo()
    }

    private fun createItemUiState(item: CategorySettingScreenSubCategoriesPagingQuery.Node): SettingCategoryScreenUiState.SubCategoryItem {
        return SettingCategoryScreenUiState.SubCategoryItem(
            name = item.name,
            event = object : SettingCategoryScreenUiState.SubCategoryItem.Event {
                override fun onClick() {
//                                            coroutineScope.launch {
//                                                viewModelEventSender.send {
//                                                    it.navigateToCategoryDetail(
//                                                        id = item.id,
//                                                    )
//                                                }
//                                            }
                }
            },
        )
    }

    private fun fetchCategoryInfo() {
        coroutineScope.launch {
            val flowResult = api.getCategoryInfo(id = categoryId)

            flowResult
                .catch {
                    globalEventSender.send {
                        it.showSnackBar("データの取得に失敗しました")
                    }
                }
                .collect { response ->
                    val categoryInfo = response.data?.user?.moneyUsageCategory ?: return@collect
                    viewModelStateFlow.update {
                        it.copy(
                            isFirstLoading = false,
                            categoryInfo = categoryInfo,
                        )
                    }
                }
        }
    }

    private fun initialFetchSubCategories() {
        coroutineScope.launch {
            val data = api.getSubCategoriesPaging(id = categoryId)?.data?.user?.moneyUsageCategory?.subCategories

            if (data == null) {
                globalEventSender.send {
                    it.showSnackBar("データの取得に失敗しました")
                }
                return@launch
            }

            viewModelStateFlow.update {
                it.copy(
                    isFirstLoading = false,
                    responseList = listOf(data),
                )
            }
        }
    }

    private data class ViewModelState(
        val isFirstLoading: Boolean,
        val responseList: List<CategorySettingScreenSubCategoriesPagingQuery.SubCategories>,
        val categoryInfo: CategorySettingScreenQuery.MoneyUsageCategory? = null,
        val showAddSubCategoryNameInput: Boolean = false,
        val showCategoryNameChangeInput: SettingCategoryScreenUiState.FullScreenInputDialog? = null,
    )

    public interface Event
}
