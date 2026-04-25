package net.matsudamper.money.frontend.common.viewmodel.settings

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.isFromCache
import net.matsudamper.money.element.MoneyUsageCategoryId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.ColorUtil
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.settings.SettingCategoryScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.CategorySettingScreenQuery
import net.matsudamper.money.frontend.graphql.CategorySettingScreenSubCategoriesPagingQuery

public class SettingCategoryViewModel(
    private val categoryId: MoneyUsageCategoryId,
    scopedObjectFeature: ScopedObjectFeature,
    private val api: SettingScreenCategoryApi,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
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

                override fun onClickBack() {
                    viewModelScope.launch {
                        viewModelEventSender.send {
                            it.navigateToCategories()
                        }
                    }
                }

                override fun onClickEditCategoryName() {
                    viewModelScope.launch {
                        viewModelStateFlow.update {
                            it.copy(isEditingCategoryName = true)
                        }
                    }
                }

                override fun onCategoryNameEditComplete(text: String) {
                    viewModelScope.launch {
                        val result = api.updateCategory(
                            id = categoryId,
                            name = Optional.present(text),
                            color = Optional.absent(),
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
                        viewModelStateFlow.update {
                            it.copy(isEditingCategoryName = false)
                        }
                    }
                }

                override fun onCategoryNameEditDismiss() {
                    viewModelScope.launch {
                        viewModelStateFlow.update {
                            it.copy(isEditingCategoryName = false)
                        }
                    }
                }

                override fun onClickChangeColor() {
                    viewModelScope.launch {
                        viewModelStateFlow.update {
                            it.copy(showColorPickerDialog = true)
                        }
                    }
                }

                override fun onDismissColorPicker() {
                    viewModelScope.launch {
                        viewModelStateFlow.update {
                            it.copy(showColorPickerDialog = false)
                        }
                    }
                }

                override fun onColorSelected(color: Color) {
                    viewModelScope.launch {
                        val result = api.updateCategory(
                            id = categoryId,
                            name = Optional.absent(),
                            color = Optional.present("#${ColorUtil.toHexColor(color)}"),
                        )?.data?.userMutation?.updateCategory
                        if (result == null) {
                            launch {
                                globalEventSender.send {
                                    it.showNativeNotification("色の変更に失敗しました")
                                }
                            }
                        } else {
                            launch {
                                globalEventSender.send {
                                    it.showSnackBar("色を変更しました")
                                }
                            }
                        }
                        viewModelStateFlow.update {
                            it.copy(showColorPickerDialog = false)
                        }
                    }
                }

                override fun onClickDeleteCategory() {
                    val state = viewModelStateFlow.value
                    val description = if (state.hasMoreSubCategories) {
                        val loadedCount = state.responseList.flatMap { it.nodes }.size
                        "${loadedCount}件以上のサブカテゴリーが紐づいています"
                    } else {
                        val subCategoryCount = state.responseList
                            .flatMap { it.nodes }
                            .size
                        if (subCategoryCount > 0) "${subCategoryCount}件のサブカテゴリーが紐づいています" else null
                    }
                    viewModelStateFlow.update {
                        it.copy(
                            confirmDialog = object : SettingCategoryScreenUiState.ConfirmDialog {
                                override val title = "このカテゴリを削除しますか"
                                override val description = description

                                override fun onConfirm() {
                                    viewModelScope.launch {
                                        val isSuccess = api.deleteCategory(id = categoryId)
                                        viewModelStateFlow.update { state ->
                                            state.copy(confirmDialog = null)
                                        }
                                        if (isSuccess) {
                                            launch {
                                                globalEventSender.send {
                                                    it.showSnackBar("削除しました")
                                                }
                                                viewModelEventSender.send {
                                                    it.navigateToCategories()
                                                }
                                            }
                                        } else {
                                            launch {
                                                globalEventSender.send {
                                                    it.showNativeNotification("削除に失敗しました")
                                                }
                                            }
                                        }
                                    }
                                }

                                override fun onDismiss() {
                                    viewModelStateFlow.update { state ->
                                        state.copy(confirmDialog = null)
                                    }
                                }
                            },
                        )
                    }
                }

                override fun onClickAddSubCategory() {
                    viewModelScope.launch {
                        viewModelStateFlow.update {
                            it.copy(isAddingSubCategory = true)
                        }
                    }
                }

                override fun onAddSubCategoryComplete(text: String) {
                    viewModelScope.launch {
                        val result = api.addSubCategory(
                            categoryId = categoryId,
                            name = text,
                        )?.data?.userMutation?.addSubCategory?.subCategory

                        viewModelStateFlow.update {
                            it.copy(isAddingSubCategory = false)
                        }

                        if (result == null) {
                            launch {
                                globalEventSender.send {
                                    it.showNativeNotification("追加に失敗しました")
                                }
                            }
                            return@launch
                        }
                        launch {
                            globalEventSender.send {
                                it.showSnackBar("${result.name}を追加しました")
                            }
                        }

                        api.refetchSubCategoriesPaging(id = categoryId)
                    }
                }

                override fun onAddSubCategoryDismiss() {
                    viewModelScope.launch {
                        viewModelStateFlow.update {
                            it.copy(isAddingSubCategory = false)
                        }
                    }
                }
            },
            loadingState = SettingCategoryScreenUiState.LoadingState.Loading,
            heroMode = SettingCategoryScreenUiState.HeroMode.Base,
            isAddingSubCategory = false,
            showColorPickerDialog = false,
            confirmDialog = null,
            categoryName = "",
            categoryColor = null,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    val loadingState = if (viewModelState.isFirstLoading) {
                        SettingCategoryScreenUiState.LoadingState.Loading
                    } else {
                        val items = viewModelState.responseList.map { it.nodes }.flatten()
                        SettingCategoryScreenUiState.LoadingState.Loaded(
                            item = items.map { item ->
                                createItemUiState(item, item.id == viewModelState.editingSubCategoryId)
                            }.toImmutableList(),
                        )
                    }

                    uiState.copy(
                        loadingState = loadingState,
                        heroMode = if (viewModelState.isEditingCategoryName) {
                            SettingCategoryScreenUiState.HeroMode.EditingCategoryName
                        } else {
                            SettingCategoryScreenUiState.HeroMode.Base
                        },
                        isAddingSubCategory = viewModelState.isAddingSubCategory,
                        showColorPickerDialog = viewModelState.showColorPickerDialog,
                        confirmDialog = viewModelState.confirmDialog,
                        categoryName = viewModelState.categoryInfo?.name.orEmpty(),
                        categoryColor = viewModelState.categoryInfo?.color?.let(ColorUtil::parseHexColor),
                    )
                }
            }
        }
    }.asStateFlow()

    private inner class CategoryDeleteDialog(
        private val item: CategorySettingScreenSubCategoriesPagingQuery.Node,
    ) : SettingCategoryScreenUiState.ConfirmDialog {
        override val title = "このサブカテゴリーを削除しますか"
        override val description = "「${item.name}」を削除します"

        override fun onConfirm() {
            viewModelScope.launch {
                val isSuccess = api.deleteSubCategory(
                    categoryId = categoryId,
                    id = item.id,
                )

                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(confirmDialog = null)
                }

                if (isSuccess.not()) {
                    launch {
                        globalEventSender.send {
                            it.showNativeNotification("削除に失敗しました")
                        }
                    }
                } else {
                    launch {
                        globalEventSender.send {
                            it.showSnackBar("削除しました")
                        }
                    }
                }
            }
        }

        override fun onDismiss() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(confirmDialog = null)
            }
        }
    }

    init {
        observeSubCategoriesPaging()
        collectCategoryInfo()
    }

    private fun createItemUiState(
        item: CategorySettingScreenSubCategoriesPagingQuery.Node,
        isEditing: Boolean,
    ): SettingCategoryScreenUiState.SubCategoryItem {
        return SettingCategoryScreenUiState.SubCategoryItem(
            name = item.name,
            isEditing = isEditing,
            event = object : SettingCategoryScreenUiState.SubCategoryItem.Event {
                override fun onClick() {
                }

                override fun onClickEdit() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(editingSubCategoryId = item.id)
                    }
                }

                override fun onEditComplete(text: String) {
                    viewModelScope.launch {
                        val result = api.updateSubCategory(
                            id = item.id,
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
                        viewModelStateFlow.update { viewModelState ->
                            viewModelState.copy(editingSubCategoryId = null)
                        }
                    }
                }

                override fun onEditDismiss() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(editingSubCategoryId = null)
                    }
                }

                override fun onClickDelete() {
                    viewModelStateFlow.update {
                        it.copy(confirmDialog = CategoryDeleteDialog(item))
                    }
                }
            },
        )
    }

    private fun collectCategoryInfo() {
        viewModelScope.launch {
            val flowResult = api.getCategoryInfo(id = categoryId)

            flowResult
                .catch {
                    globalEventSender.send {
                        it.showSnackBar("データの取得に失敗しました")
                    }
                }
                .collect { response ->
                    val categoryInfo = response.data?.user?.moneyUsageCategory
                    if (categoryInfo == null) {
                        if (response.isFromCache && response.data == null) return@collect
                        globalEventSender.send {
                            it.showSnackBar("データの取得に失敗しました")
                        }
                        return@collect
                    }
                    viewModelStateFlow.update {
                        it.copy(
                            isFirstLoading = false,
                            categoryInfo = categoryInfo,
                        )
                    }
                }
        }
    }

    private fun observeSubCategoriesPaging() {
        viewModelScope.launch {
            api.getSubCategoriesPaging(id = categoryId)
                .collect { response ->
                    val data = response.data?.user?.moneyUsageCategory?.subCategories
                    if (data == null) {
                        if (response.isFromCache && response.data == null) return@collect
                        globalEventSender.send {
                            it.showSnackBar("データの取得に失敗しました")
                        }
                        return@collect
                    }

                    viewModelStateFlow.update {
                        it.copy(
                            isFirstLoading = false,
                            responseList = listOf(data),
                            hasMoreSubCategories = data.cursor != null,
                        )
                    }
                }
        }
    }

    private data class ViewModelState(
        val isFirstLoading: Boolean,
        val responseList: List<CategorySettingScreenSubCategoriesPagingQuery.SubCategories>,
        val hasMoreSubCategories: Boolean = false,
        val categoryInfo: CategorySettingScreenQuery.MoneyUsageCategory? = null,
        val isEditingCategoryName: Boolean = false,
        val editingSubCategoryId: MoneyUsageSubCategoryId? = null,
        val isAddingSubCategory: Boolean = false,
        val showColorPickerDialog: Boolean = false,
        val confirmDialog: SettingCategoryScreenUiState.ConfirmDialog? = null,
    )

    public interface Event {
        public fun navigateToCategories()
    }
}
