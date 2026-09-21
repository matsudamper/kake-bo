package net.matsudamper.money.frontend.common.viewmodel.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.cache.normalized.isFromCache
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.ColorUtil
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingSubCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.SubCategorySettingScreenQuery

public class SettingSubCategoryViewModel(
    private val subCategoryId: MoneyUsageSubCategoryId,
    scopedObjectFeature: ScopedObjectFeature,
    private val api: SettingScreenSubCategoryApi,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(ViewModelState())

    private val viewModelEventSender = EventSender<Event>()
    public val viewModelEventHandler: EventHandler<Event> = viewModelEventSender.asHandler()

    private val globalEventSender = EventSender<GlobalEvent>()
    public val globalEventHandler: EventHandler<GlobalEvent> = globalEventSender.asHandler()

    public val uiState: StateFlow<SettingSubCategoryScreenUiState> = MutableStateFlow(
        SettingSubCategoryScreenUiState(
            event = object : SettingSubCategoryScreenUiState.Event {
                override suspend fun onResume() {
                }

                override fun onClickBack() {
                    if (clearEditModeIfNeeded()) {
                        return
                    }

                    val categoryId = viewModelStateFlow.value.subCategoryInfo?.category?.id
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.navigateBack(categoryId)
                        }
                    }
                }

                override fun onClickEditSubCategoryName() {
                    viewModelStateFlow.update {
                        it.copy(isEditingSubCategoryName = true)
                    }
                }

                override fun onSubCategoryNameEditComplete(text: String) {
                    viewModelScope.launch {
                        val result = api.updateSubCategory(
                            id = subCategoryId,
                            name = text,
                        )?.data?.userMutation?.updateSubCategory
                        if (result == null) {
                            launch {
                                globalEventSender.send {
                                    it.showNativeNotification("サブカテゴリ名の変更に失敗しました")
                                }
                            }
                        } else {
                            launch {
                                globalEventSender.send {
                                    it.showSnackBar("サブカテゴリ名を変更しました")
                                }
                            }
                        }
                        viewModelStateFlow.update {
                            it.copy(isEditingSubCategoryName = false)
                        }
                    }
                }

                override fun onClickCategory() {
                    val categoryId = viewModelStateFlow.value.subCategoryInfo?.category?.id ?: return
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.navigateToCategory(categoryId)
                        }
                    }
                }
            },
            loadingState = SettingSubCategoryScreenUiState.LoadingState.Loading,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                val subCategoryInfo = viewModelState.subCategoryInfo
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = if (subCategoryInfo == null) {
                            if (viewModelState.isLoadFailed) {
                                SettingSubCategoryScreenUiState.LoadingState.Error
                            } else {
                                SettingSubCategoryScreenUiState.LoadingState.Loading
                            }
                        } else {
                            SettingSubCategoryScreenUiState.LoadingState.Loaded(
                                subCategoryName = subCategoryInfo.name,
                                categoryName = subCategoryInfo.category.name,
                                categoryColor = subCategoryInfo.category.color?.let(ColorUtil::parseHexColor),
                                heroMode = if (viewModelState.isEditingSubCategoryName) {
                                    SettingSubCategoryScreenUiState.HeroMode.EditingSubCategoryName
                                } else {
                                    SettingSubCategoryScreenUiState.HeroMode.Base
                                },
                            )
                        },
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        collectSubCategoryInfo()
    }

    private fun collectSubCategoryInfo() {
        viewModelScope.launch {
            api.getSubCategoryInfo(id = subCategoryId)
                .catch {
                    viewModelStateFlow.update { state -> state.copy(isLoadFailed = true) }
                    globalEventSender.send {
                        it.showSnackBar("データの取得に失敗しました")
                    }
                }
                .collect { response ->
                    val subCategoryInfo = response.data?.user?.moneyUsageSubCategory
                    if (subCategoryInfo == null) {
                        if (response.isFromCache && response.data == null) return@collect
                        viewModelStateFlow.update { state ->
                            state.copy(
                                subCategoryInfo = null,
                                isLoadFailed = true,
                            )
                        }
                        globalEventSender.send {
                            it.showSnackBar("データの取得に失敗しました")
                        }
                        return@collect
                    }
                    viewModelStateFlow.update {
                        it.copy(
                            subCategoryInfo = subCategoryInfo,
                            isLoadFailed = false,
                        )
                    }
                }
        }
    }

    private fun clearEditModeIfNeeded(): Boolean {
        if (!viewModelStateFlow.value.isEditingSubCategoryName) {
            return false
        }

        viewModelStateFlow.update {
            it.copy(isEditingSubCategoryName = false)
        }
        return true
    }

    private data class ViewModelState(
        val subCategoryInfo: SubCategorySettingScreenQuery.MoneyUsageSubCategory? = null,
        val isEditingSubCategoryName: Boolean = false,
        val isLoadFailed: Boolean = false,
    )

    public interface Event {
        public fun navigateToCategory(id: MoneyUsageCategoryId)

        /**
         * 読み込み前・失敗時は親カテゴリが分からないため、categoryIdはnullになりうる
         */
        public fun navigateBack(categoryId: MoneyUsageCategoryId?)
    }
}
