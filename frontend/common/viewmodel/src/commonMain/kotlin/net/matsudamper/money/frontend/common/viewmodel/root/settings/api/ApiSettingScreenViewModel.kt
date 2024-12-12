package net.matsudamper.money.frontend.common.viewmodel.root.settings.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ApiSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.PlatformType
import net.matsudamper.money.frontend.common.viewmodel.PlatformTypeProvider
import net.matsudamper.money.frontend.common.viewmodel.RootScreenScaffoldListenerDefaultImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.ApiSettingScreenQuery
import net.matsudamper.money.frontend.graphql.ApiSettingScreenRegisterApiTokenMutation

public class ApiSettingScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val api: ApiSettingScreenApi,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val loadedEvent = object : ApiSettingScreenUiState.LoadedEvent {
        override fun onClickAddToken() {
            viewModelStateFlow.value = viewModelStateFlow.value.copy(
                addDialogUiState = ApiSettingScreenUiState.AddDialogUiState(
                    event = object : ApiSettingScreenUiState.AddDialogUiState.Event {
                        override fun onComplete(name: String) {
                            viewModelScope.launch {
                                val result = api.registerToken(name)
                                viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                    addTokenResult = result.getOrNull()?.data,
                                )
                                result
                                    .onSuccess {
                                        dismiss()
                                        api.get().first()
                                    }
                                    .onFailure {
                                        eventSender.send {
                                            it.showToast("トークンの追加に失敗しました")
                                        }
                                    }
                            }
                        }

                        override fun dismissRequest() {
                            dismiss()
                        }

                        private fun dismiss() {
                            viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                addDialogUiState = null,
                            )
                        }
                    },
                ),
            )
        }
    }

    private val rootScreenScaffoldListener = object : RootScreenScaffoldListenerDefaultImpl(navController) {
        override fun onClickSettings() {
            if (PlatformTypeProvider.type == PlatformType.JS) {
                super.onClickSettings()
            }
        }
    }

    public val uiStateFlow: StateFlow<ApiSettingScreenUiState> = MutableStateFlow(
        ApiSettingScreenUiState(
            event = object : ApiSettingScreenUiState.Event {
                override suspend fun onViewInitialized() {
                    withContext(viewModelScope.coroutineContext) {
                        launch {
                            api.get()
                                .catch {
                                    viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                        loadingState = ViewModelState.LoadingState.Error,
                                    )
                                }
                                .collect {
                                    val tokens = it.data?.user?.settings?.apiTokenAttributes?.apiTokens
                                    if (tokens == null) {
                                        viewModelStateFlow.update { viewModelState ->
                                            viewModelState.copy(
                                                loadingState = ViewModelState.LoadingState.Error,
                                            )
                                        }
                                        return@collect
                                    }
                                    viewModelStateFlow.update { viewModelState ->
                                        viewModelState.copy(
                                            tokens = tokens,
                                            loadingState = ViewModelState.LoadingState.Loaded,
                                        )
                                    }
                                }
                        }
                    }
                }

                override fun onClickReloadButton() {
                    viewModelScope.launch {
                        viewModelStateFlow.value = viewModelStateFlow.value.copy(
                            loadingState = ViewModelState.LoadingState.Loading,
                        )
                        api.get().first()
                    }
                }

                override fun onClickBack() {
                    viewModelScope.launch {
                        eventSender.send {
                            it.navigate(ScreenStructure.Root.Settings.Root)
                        }
                    }
                }
            },
            loadingState = ApiSettingScreenUiState.LoadingState.Loading,
            addDialog = null,
            addTokenResult = null,
            rootScreenScaffoldListener = rootScreenScaffoldListener,
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.value = ApiSettingScreenUiState(
                    loadingState = when (viewModelState.loadingState) {
                        ViewModelState.LoadingState.Loading -> ApiSettingScreenUiState.LoadingState.Loading
                        ViewModelState.LoadingState.Error -> ApiSettingScreenUiState.LoadingState.Error
                        ViewModelState.LoadingState.Loaded -> {
                            ApiSettingScreenUiState.LoadingState.Loaded(
                                tokens = viewModelState.tokens.map { token ->
                                    ApiSettingScreenUiState.Token(
                                        name = token.name,
                                        expiresAt = run expire@{
                                            val expire = token.expiresAt ?: return@expire "期限なし"

                                            Instant.fromEpochMilliseconds(expire.toEpochMilliseconds())
                                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                                .toString()
                                        },
                                    )
                                },
                                event = loadedEvent,
                            )
                        }
                    },
                    event = uiStateFlow.value.event,
                    addDialog = viewModelState.addDialogUiState,
                    rootScreenScaffoldListener = rootScreenScaffoldListener,
                    addTokenResult = viewModelState.addTokenResult?.let {
                        val token = it.userMutation.registerApiToken.apiToken ?: return@let null
                        ApiSettingScreenUiState.AddTokenResult(
                            name = token,
                            event = object : ApiSettingScreenUiState.AddTokenResult.Event {
                                override fun dismiss() {
                                    viewModelStateFlow.value = viewModelStateFlow.value.copy(
                                        addTokenResult = null,
                                    )
                                }

                                override fun onClickCopy() {
                                    viewModelScope.launch {
                                        eventSender.send { event ->
                                            event.copyToClipboard(token)
                                            event.showToast("トークンをコピーしました")
                                        }
                                    }
                                }
                            },
                        )
                    },
                )
            }
        }
    }.asStateFlow()

    public interface Event {
        public fun navigate(structure: ScreenStructure)
        public fun showToast(text: String)
        public fun copyToClipboard(token: String)
        public suspend fun showSnackbar(text: String)
    }

    private data class ViewModelState(
        val tokens: List<ApiSettingScreenQuery.ApiToken> = listOf(),
        val loadingState: LoadingState = LoadingState.Loading,
        val addDialogUiState: ApiSettingScreenUiState.AddDialogUiState? = null,
        val addTokenResult: ApiSettingScreenRegisterApiTokenMutation.Data? = null,
    ) {
        enum class LoadingState {
            Loading,
            Loaded,
            Error,
        }
    }
}
