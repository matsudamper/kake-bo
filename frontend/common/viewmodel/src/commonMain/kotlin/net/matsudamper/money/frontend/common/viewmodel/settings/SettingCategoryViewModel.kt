package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.ui.screen.settings.SettingCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.SubCategorySettingScreenQuery

public class SettingCategoryViewModel(
    private val categoryId: MoneyUsageCategoryId,
    private val coroutineScope: CoroutineScope,
    private val api: SettingCategoryApi,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            responseList = listOf(),
            isFirstLoading = true,
            showCategoryNameInput = false,
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
                                showCategoryNameInput = false,
                            )
                        }
                    }
                }

                override fun onClickAddSubCategoryButton() {
                    coroutineScope.launch {
                        viewModelStateFlow.update {
                            it.copy(
                                showCategoryNameInput = true,
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
                                showCategoryNameInput = false,
                            )
                        }

                        initialFetch()
                    }
                }
            },
            loadingState = SettingCategoryScreenUiState.LoadingState.Loading,
            showCategoryNameInput = false,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    val loadingState = if (viewModelState.isFirstLoading) {
                        SettingCategoryScreenUiState.LoadingState.Loading
                    } else {
                        val items = viewModelState.responseList.mapNotNull {
                            it.user?.moneyUsageSubCategoriesFromCategoryId?.nodes
                        }.flatten()
                        SettingCategoryScreenUiState.LoadingState.Loaded(
                            item = items.map { item ->
                                SettingCategoryScreenUiState.SubCategoryItem(
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
            val data = api.getSubCategory(id = categoryId)?.data

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
        val responseList: List<SubCategorySettingScreenQuery.Data>,
        val showCategoryNameInput: Boolean,
    )

    public interface Event
}
