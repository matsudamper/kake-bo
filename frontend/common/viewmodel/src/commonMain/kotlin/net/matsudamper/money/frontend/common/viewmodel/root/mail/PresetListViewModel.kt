package net.matsudamper.money.frontend.common.viewmodel.root.mail

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.mail.PresetListScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.settings.PresetScreenApi
import net.matsudamper.money.frontend.graphql.GetMoneyUsagePresetsQuery

public class PresetListViewModel(
    private val api: PresetScreenApi,
    scopedObjectFeature: ScopedObjectFeature,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            presets = listOf(),
            isLoading = true,
            showNameInput = false,
            isError = false,
        ),
    )

    private val globalEventSender = EventSender<GlobalEvent>()
    public val globalEventHandler: EventHandler<GlobalEvent> = globalEventSender.asHandler()

    public val uiStateFlow: StateFlow<PresetListScreenUiState> = MutableStateFlow(
        PresetListScreenUiState(
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            loadingState = PresetListScreenUiState.LoadingState.Loading,
            showNameInput = false,
            event = object : PresetListScreenUiState.Event {
                override fun onResume() {
                    fetchCollect()
                }

                override fun onClickAddButton() {
                    viewModelStateFlow.update { it.copy(showNameInput = true) }
                }

                override fun onNameInputCompleted(name: String) {
                    viewModelScope.launch {
                        viewModelStateFlow.update { it.copy(showNameInput = false) }
                        val result = api.addPreset(name = name, subCategoryId = null)
                        val preset = result?.data?.userMutation?.addMoneyUsagePreset?.preset
                        if (preset == null) {
                            globalEventSender.send { it.showNativeNotification("追加に失敗しました") }
                            return@launch
                        }
                        navController.navigate(
                            ScreenStructure.Root.Add.PresetDetail(
                                id = preset.id,
                            ),
                        )
                    }
                }

                override fun onDismissNameInput() {
                    viewModelStateFlow.update { it.copy(showNameInput = false) }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    val loadingState = if (viewModelState.isLoading) {
                        PresetListScreenUiState.LoadingState.Loading
                    } else if (viewModelState.isError) {
                        PresetListScreenUiState.LoadingState.Error
                    } else {
                        PresetListScreenUiState.LoadingState.Loaded(
                            items = viewModelState.presets.map { preset ->
                                PresetListScreenUiState.PresetItem(
                                    name = preset.name,
                                    subCategoryName = preset.subCategory?.name,
                                    event = object : PresetListScreenUiState.PresetItem.Event {
                                        override fun onClick() {
                                            navController.navigate(
                                                ScreenStructure.AddMoneyUsage(
                                                    title = preset.name,
                                                    subCategoryId = preset.subCategory?.id?.id?.toString(),
                                                ),
                                            )
                                        }

                                        override fun onClickDelete() {
                                            viewModelScope.launch {
                                                val deleted = api.deletePreset(preset.id)
                                                if (!deleted) {
                                                    globalEventSender.send {
                                                        it.showNativeNotification("削除に失敗しました")
                                                    }
                                                    return@launch
                                                }
                                                fetchCollect()
                                            }
                                        }

                                        override fun onClickEdit() {
                                            navController.navigate(
                                                ScreenStructure.Root.Add.PresetDetail(
                                                    id = preset.id,
                                                ),
                                            )
                                        }
                                    },
                                )
                            }.toImmutableList(),
                        )
                    }
                    uiState.copy(
                        loadingState = loadingState,
                        showNameInput = viewModelState.showNameInput,
                    )
                }
            }
        }
    }.asStateFlow()

    private var fetchJob: Job = Job()
    private fun fetchCollect() {
        fetchJob.cancel()
        fetchJob = viewModelScope.launch {
            viewModelStateFlow.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                )
            }
            api.getPresets()
                .catch {
                    globalEventSender.send { it.showSnackBar("データの取得に失敗しました") }
                    viewModelStateFlow.update {
                        it.copy(
                            isLoading = false,
                            isError = true,
                        )
                    }
                }
                .collectLatest { response ->
                    viewModelStateFlow.update {
                        it.copy(
                            isLoading = false,
                            isError = false,
                            presets = response.data?.user?.moneyUsagePresets.orEmpty(),
                        )
                    }
                }
        }
    }

    private data class ViewModelState(
        val isLoading: Boolean,
        val presets: List<GetMoneyUsagePresetsQuery.MoneyUsagePreset>,
        val isError: Boolean,
        val showNameInput: Boolean,
    )
}
