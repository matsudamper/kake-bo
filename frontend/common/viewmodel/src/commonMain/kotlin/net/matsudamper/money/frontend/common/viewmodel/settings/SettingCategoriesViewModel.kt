package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingCategoriesScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.CategoriesSettingScreenCategoriesPagingQuery

public class SettingCategoriesViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: SettingScreenCategoryApi,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            responseList = listOf(),
            isFirstLoading = true,
            showCategoryNameInput = false,
        ),
    )

    private val viewModelEventSender = EventSender<SettingCategoriesViewModelEvent>()
    public val viewModelEventHandler: EventHandler<SettingCategoriesViewModelEvent> = viewModelEventSender.asHandler()

    private val globalEventSender = EventSender<GlobalEvent>()
    public val globalEventHandler: EventHandler<GlobalEvent> = globalEventSender.asHandler()

    public val uiState: StateFlow<SettingCategoriesScreenUiState> = MutableStateFlow(
        SettingCategoriesScreenUiState(
            event = object : SettingCategoriesScreenUiState.Event {
                override suspend fun onResume() {
                }

                override fun dismissCategoryInput() {
                    coroutineScope.launch {
                        viewModelStateFlow.update {
                            it.copy(
                                showCategoryNameInput = false,
                            )
                        }
                    }
                }

                override fun onClickAddCategoryButton() {
                    coroutineScope.launch {
                        viewModelStateFlow.update {
                            it.copy(
                                showCategoryNameInput = true,
                            )
                        }
                    }
                }

                override fun categoryInputCompleted(text: String) {
                    coroutineScope.launch {
                        val result = api.addCategory(text)?.data?.userMutation?.addCategory?.category
                        if (result == null) {
                            globalEventSender.send {
                                it.showNativeNotification("追加に失敗しました")
                            }
                            return@launch
                        }

                        globalEventSender.send {
                            it.showSnackBar("${result.name}を追加しました")
                        }
                        viewModelStateFlow.update {
                            it.copy(
                                showCategoryNameInput = false,
                            )
                        }

                        initialFetch()
                    }
                }
            },
            loadingState = SettingCategoriesScreenUiState.LoadingState.Loading,
            showCategoryNameInput = false,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    val loadingState = if (viewModelState.isFirstLoading) {
                        SettingCategoriesScreenUiState.LoadingState.Loading
                    } else {
                        val items = viewModelState.responseList.mapNotNull {
                            it.user?.moneyUsageCategories?.nodes
                        }.flatten()
                        SettingCategoriesScreenUiState.LoadingState.Loaded(
                            item = items.map { item ->
                                SettingCategoriesScreenUiState.CategoryItem(
                                    name = item.name,
                                    event = object : SettingCategoriesScreenUiState.CategoryItem.Event {
                                        override fun onClick() {
                                            coroutineScope.launch {
                                                viewModelEventSender.send {
                                                    it.navigateToCategoryDetail(
                                                        id = item.id,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                )
                            }.toImmutableList(),
                        )
                    }

                    uiState.copy(
                        loadingState = loadingState,
                        showCategoryNameInput = viewModelState.showCategoryNameInput,
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        initialFetch()
    }

    private fun initialFetch() {
        coroutineScope.launch {
            val data = api.getCategories()?.data

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
        val responseList: List<CategoriesSettingScreenCategoriesPagingQuery.Data>,
        val showCategoryNameInput: Boolean,
    )
}

public interface SettingCategoriesViewModelEvent {
    public fun navigateToCategoryDetail(id: MoneyUsageCategoryId)
}
