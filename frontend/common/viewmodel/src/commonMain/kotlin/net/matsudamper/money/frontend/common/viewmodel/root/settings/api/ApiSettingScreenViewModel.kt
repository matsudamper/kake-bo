package net.matsudamper.money.frontend.common.viewmodel.root.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.internal.JSJoda.OffsetDateTime
import kotlinx.datetime.internal.JSJoda.ZoneId
import kotlinx.datetime.toLocalDateTime
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ApiSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.ApiSettingScreenQuery

public class ApiSettingScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: ApiSettingScreenApi,
) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<ApiSettingScreenUiState> = MutableStateFlow(
        ApiSettingScreenUiState(
            event = object : ApiSettingScreenUiState.Event {
                override suspend fun onViewInitialized() {
                    withContext(coroutineScope.coroutineContext) {
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
                    coroutineScope.launch {
                        viewModelStateFlow.value = viewModelStateFlow.value.copy(
                            loadingState = ViewModelState.LoadingState.Loading,
                        )
                        api.get().first()
                    }
                }

                override fun onClickBack() {
                    coroutineScope.launch {
                        eventSender.send {
                            it.navigate(ScreenStructure.Root.Settings.Root)
                        }
                    }
                }
            },
            loadingState = ApiSettingScreenUiState.LoadingState.Loading,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
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
                            )
                        }
                    },
                    event = uiStateFlow.value.event,
                )
            }
        }
    }.asStateFlow()

    public interface Event {
        public fun navigate(structure: ScreenStructure)
    }

    private data class ViewModelState(
        val tokens: List<ApiSettingScreenQuery.ApiToken> = listOf(),
        val loadingState: LoadingState = LoadingState.Loading,
    ) {
        enum class LoadingState {
            Loading,
            Loaded,
            Error,
        }
    }
}