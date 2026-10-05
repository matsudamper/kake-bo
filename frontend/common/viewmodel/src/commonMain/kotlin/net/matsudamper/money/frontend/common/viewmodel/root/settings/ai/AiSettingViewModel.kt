package net.matsudamper.money.frontend.common.viewmodel.root.settings.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.screen.root.settings.AiSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent

private const val TAG = "AiSettingViewModel"

public class AiSettingViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlApi: AiSettingGraphqlApi,
    private val globalEventSender: EventSender<GlobalEvent>,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AiSettingScreenUiState> = MutableStateFlow(
        AiSettingScreenUiState(
            loadingState = AiSettingScreenUiState.LoadingState.Loading,
            isGeminiApiKeyInputVisible = false,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            event = object : AiSettingScreenUiState.Event {
                override fun onClickBack() {
                    navController.back()
                }

                override fun onResume() {
                    load()
                }

                override fun onClickRetry() {
                    load()
                }

                override fun onClickChangeGeminiApiKey() {
                    viewModelStateFlow.update { it.copy(isGeminiApiKeyInputVisible = true) }
                }

                override fun onCompleteGeminiApiKeyInput(text: String) {
                    updateGeminiApiKey(text)
                }

                override fun onCancelGeminiApiKeyInput() {
                    viewModelStateFlow.update { it.copy(isGeminiApiKeyInputVisible = false) }
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update {
                    it.copy(
                        loadingState = when (viewModelState.loadingState) {
                            ViewModelState.LoadingState.Loading -> AiSettingScreenUiState.LoadingState.Loading
                            ViewModelState.LoadingState.Error -> AiSettingScreenUiState.LoadingState.Error
                            ViewModelState.LoadingState.Loaded -> AiSettingScreenUiState.LoadingState.Loaded(
                                geminiApiKey = if (viewModelState.hasGeminiApiKey) "****************" else "",
                            )
                        },
                        isGeminiApiKeyInputVisible = viewModelState.isGeminiApiKeyInputVisible,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun load() {
        viewModelScope.launch {
            val hasGeminiApiKey = runCatching { graphqlApi.getAiConfig() }
                .onFailure { Logger.e(TAG, it) }
                .getOrNull()
                ?.data?.user?.settings?.aiConfig?.hasGeminiApiKey
            viewModelStateFlow.update { viewModelState ->
                when {
                    hasGeminiApiKey != null -> viewModelState.copy(
                        hasGeminiApiKey = hasGeminiApiKey,
                        loadingState = ViewModelState.LoadingState.Loaded,
                    )

                    viewModelState.loadingState == ViewModelState.LoadingState.Loaded -> viewModelState

                    else -> viewModelState.copy(loadingState = ViewModelState.LoadingState.Error)
                }
            }
        }
    }

    private fun updateGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            val aiConfig = runCatching { graphqlApi.setGeminiApiKey(apiKey = apiKey) }
                .onFailure { Logger.e(TAG, it) }
                .getOrNull()
                ?.data?.userMutation?.settingsMutation?.updateGeminiApiKey
            if (aiConfig == null) {
                globalEventSender.send {
                    it.showNativeNotification("更新に失敗しました")
                }
                return@launch
            }
            viewModelStateFlow.update {
                it.copy(
                    hasGeminiApiKey = aiConfig.hasGeminiApiKey,
                    isGeminiApiKeyInputVisible = false,
                )
            }
        }
    }

    private data class ViewModelState(
        val hasGeminiApiKey: Boolean = false,
        val isGeminiApiKeyInputVisible: Boolean = false,
        val loadingState: LoadingState = LoadingState.Loading,
    ) {
        enum class LoadingState {
            Loading,
            Loaded,
            Error,
        }
    }
}
