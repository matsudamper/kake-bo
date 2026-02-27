package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.apollographql.apollo.api.Optional
import net.matsudamper.money.element.MoneyUsagePresetId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.ui.base.CategorySelectDialogUiState
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.NumberInputValue
import net.matsudamper.money.frontend.common.ui.screen.root.mail.PresetDetailScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.layout.CategorySelectDialogViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.settings.PresetScreenApi
import net.matsudamper.money.frontend.graphql.GetMoneyUsagePresetQuery
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.UpdateMoneyUsagePresetMutation

public class PresetDetailViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val presetId: MoneyUsagePresetId,
    private val api: PresetScreenApi,
    navController: ScreenNavController,
    graphqlClient: GraphqlClient,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val globalEventSender = EventSender<GlobalEvent>()
    public val globalEventHandler: EventHandler<GlobalEvent> = globalEventSender.asHandler()

    private val categorySelectDialogViewModel = object {
        private val event: CategorySelectDialogViewModel.Event = object : CategorySelectDialogViewModel.Event {
            override fun selected(result: CategorySelectDialogViewModel.SelectedResult) {
                viewModelScope.launch {
                    val updateResult = api.updatePreset(
                        id = presetId,
                        subCategoryId = Optional.present(result.subCategoryId),
                    )
                    val updatedPreset = updateResult?.data?.userMutation?.updateMoneyUsagePreset
                    if (updatedPreset == null) {
                        showScreenError()
                        return@launch
                    }
                    viewModel.dismissDialog()
                    viewModelStateFlow.update { state ->
                        state.copy(preset = updatedPreset.toViewModelPreset())
                    }
                }
            }
        }

        val viewModel = CategorySelectDialogViewModel(
            scopedObjectFeature = scopedObjectFeature,
            apolloClient = graphqlClient.apolloClient,
            event = event,
        )
    }.viewModel

    public val uiStateFlow: StateFlow<PresetDetailScreenUiState> = MutableStateFlow(
        PresetDetailScreenUiState(
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            loadingState = PresetDetailScreenUiState.LoadingState.Loading,
            showNameChangeDialog = null,
            numberInputDialog = null,
            showDescriptionChangeDialog = null,
            categorySelectDialog = null,
            event = object : PresetDetailScreenUiState.Event {
                override fun onResume() {
                    fetch()
                }

                override fun onClickRetry() {
                    fetch()
                }

                override fun onRefresh() {
                    fetch()
                }

                override fun onClickPresetNameChange() {
                    val preset = viewModelStateFlow.value.preset ?: return
                    viewModelStateFlow.update { state ->
                        state.copy(
                            showNameChangeDialog = PresetDetailScreenUiState.FullScreenInputDialog(
                                defaultText = preset.name,
                                event = object : PresetDetailScreenUiState.FullScreenInputDialog.Event {
                                    override fun onDismiss() {
                                        dismissNameChangeDialog()
                                    }

                                    override fun onCompleted(text: String) {
                                        viewModelScope.launch {
                                            val updateResult = api.updatePreset(
                                                id = presetId,
                                                name = Optional.present(text),
                                            )
                                            val updatedPreset = updateResult?.data?.userMutation?.updateMoneyUsagePreset
                                            if (updatedPreset == null) {
                                                showScreenError()
                                                return@launch
                                            }
                                            dismissNameChangeDialog()
                                            viewModelStateFlow.update { state ->
                                                state.copy(preset = updatedPreset.toViewModelPreset())
                                            }
                                        }
                                    }
                                },
                            ),
                        )
                    }
                }

                override fun onClickSubCategoryChange() {
                    val preset = viewModelStateFlow.value.preset
                    categorySelectDialogViewModel.showDialog(
                        categoryId = preset?.categoryId,
                        categoryName = preset?.categoryName,
                        subCategoryId = preset?.subCategoryId,
                        subCategoryName = preset?.subCategoryName,
                    )
                }

                override fun onClickAmountChange() {
                    val preset = viewModelStateFlow.value.preset ?: return
                    viewModelStateFlow.update { state ->
                        state.copy(
                            numberInputDialog = PresetDetailScreenUiState.NumberInputDialog(
                                value = NumberInputValue.default(preset.amount ?: 0),
                                onChangeValue = { value ->
                                    viewModelStateFlow.update { s ->
                                        s.copy(
                                            numberInputDialog = s.numberInputDialog?.copy(value = value),
                                        )
                                    }
                                },
                                dismissRequest = {
                                    val value = viewModelStateFlow.value.numberInputDialog?.value
                                    dismissNumberInputDialog()
                                    if (value == null) return@NumberInputDialog
                                    viewModelScope.launch {
                                        val updateResult = api.updatePreset(
                                            id = presetId,
                                            amount = Optional.present(value.value),
                                        )
                                        val updatedPreset = updateResult?.data?.userMutation?.updateMoneyUsagePreset
                                        if (updatedPreset == null) {
                                            showScreenError()
                                            return@launch
                                        }
                                        viewModelStateFlow.update { state ->
                                            state.copy(preset = updatedPreset.toViewModelPreset())
                                        }
                                    }
                                },
                            ),
                        )
                    }
                }

                override fun onClickDescriptionChange() {
                    val preset = viewModelStateFlow.value.preset ?: return
                    viewModelStateFlow.update { state ->
                        state.copy(
                            showDescriptionChangeDialog = PresetDetailScreenUiState.FullScreenInputDialog(
                                defaultText = preset.description.orEmpty(),
                                event = object : PresetDetailScreenUiState.FullScreenInputDialog.Event {
                                    override fun onDismiss() {
                                        dismissDescriptionChangeDialog()
                                    }

                                    override fun onCompleted(text: String) {
                                        viewModelScope.launch {
                                            val description = text.takeIf { it.isNotBlank() }
                                            val updateResult = api.updatePreset(
                                                id = presetId,
                                                description = Optional.present(description),
                                            )
                                            val updatedPreset = updateResult?.data?.userMutation?.updateMoneyUsagePreset
                                            if (updatedPreset == null) {
                                                showScreenError()
                                                return@launch
                                            }
                                            dismissDescriptionChangeDialog()
                                            viewModelStateFlow.update { state ->
                                                state.copy(preset = updatedPreset.toViewModelPreset())
                                            }
                                        }
                                    }
                                },
                            ),
                        )
                    }
                }

                override fun onClickBack() {
                    navController.back()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { state ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        loadingState = when {
                            state.isLoading -> PresetDetailScreenUiState.LoadingState.Loading
                            state.isError -> PresetDetailScreenUiState.LoadingState.Error
                            else -> PresetDetailScreenUiState.LoadingState.Loaded(
                                presetName = state.preset?.name.orEmpty(),
                                subCategoryName = state.preset?.subCategoryName ?: "未設定",
                                amount = state.preset?.amount,
                                description = state.preset?.description,
                            )
                        },
                        showNameChangeDialog = state.showNameChangeDialog,
                        numberInputDialog = state.numberInputDialog,
                        showDescriptionChangeDialog = state.showDescriptionChangeDialog,
                        categorySelectDialog = state.categorySelectDialog,
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        viewModelScope.launch {
            categorySelectDialogViewModel.getUiStateFlow().collectLatest { categorySelectDialog ->
                viewModelStateFlow.update { state ->
                    state.copy(
                        categorySelectDialog = categorySelectDialog,
                    )
                }
            }
        }
    }

    private fun fetch() {
        viewModelScope.launch {
            viewModelStateFlow.update { it.copy(isLoading = true, isError = false) }
            val preset = api.getPreset(id = presetId)?.data?.user?.moneyUsagePreset?.toViewModelPreset()
            if (preset == null) {
                showScreenError()
                return@launch
            }
            viewModelStateFlow.update {
                it.copy(
                    isLoading = false,
                    isError = false,
                    preset = ViewModelPreset(
                        name = preset.name,
                        categoryId = preset.categoryId,
                        categoryName = preset.categoryName,
                        subCategoryId = preset.subCategoryId,
                        subCategoryName = preset.subCategoryName,
                        amount = preset.amount,
                        description = preset.description,
                    ),
                )
            }
        }
    }

    private fun showScreenError() {
        viewModelStateFlow.update {
            it.copy(
                isLoading = false,
                isError = true,
                showNameChangeDialog = null,
                numberInputDialog = null,
                showDescriptionChangeDialog = null,
                categorySelectDialog = null,
            )
        }
    }

    private fun dismissNameChangeDialog() {
        viewModelStateFlow.update { state ->
            state.copy(showNameChangeDialog = null)
        }
    }

    private fun dismissNumberInputDialog() {
        viewModelStateFlow.update { state ->
            state.copy(numberInputDialog = null)
        }
    }

    private fun dismissDescriptionChangeDialog() {
        viewModelStateFlow.update { state ->
            state.copy(showDescriptionChangeDialog = null)
        }
    }

    private data class ViewModelState(
        val isLoading: Boolean = true,
        val isError: Boolean = false,
        val preset: ViewModelPreset? = null,
        val showNameChangeDialog: PresetDetailScreenUiState.FullScreenInputDialog? = null,
        val numberInputDialog: PresetDetailScreenUiState.NumberInputDialog? = null,
        val showDescriptionChangeDialog: PresetDetailScreenUiState.FullScreenInputDialog? = null,
        val categorySelectDialog: CategorySelectDialogUiState? = null,
    )

    private data class ViewModelPreset(
        val name: String,
        val categoryId: net.matsudamper.money.element.MoneyUsageCategoryId?,
        val categoryName: String?,
        val subCategoryId: MoneyUsageSubCategoryId?,
        val subCategoryName: String?,
        val amount: Int?,
        val description: String?,
    )

    private fun GetMoneyUsagePresetQuery.MoneyUsagePreset.toViewModelPreset(): ViewModelPreset {
        return ViewModelPreset(
            name = name,
            categoryId = subCategory?.category?.id,
            categoryName = subCategory?.category?.name,
            subCategoryId = subCategory?.id,
            subCategoryName = subCategory?.name,
            amount = amount,
            description = description,
        )
    }

    private fun UpdateMoneyUsagePresetMutation.UpdateMoneyUsagePreset.toViewModelPreset(): ViewModelPreset {
        return ViewModelPreset(
            name = name,
            categoryId = subCategory?.category?.id,
            categoryName = subCategory?.category?.name,
            subCategoryId = subCategory?.id,
            subCategoryName = subCategory?.name,
            amount = amount,
            description = description,
        )
    }
}
