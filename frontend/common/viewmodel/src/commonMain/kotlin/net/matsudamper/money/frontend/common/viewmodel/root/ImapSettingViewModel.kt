package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImapSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery
import net.matsudamper.money.frontend.graphql.fragment.DisplayImapConfig

private const val TAG = "ImapSettingViewModel"

public class ImapSettingViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlQuery: GraphqlUserConfigQuery,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val ioDispatchers: CoroutineDispatcher,
    navController: ScreenNavController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiState: StateFlow<ImapSettingScreenUiState> = MutableStateFlow(
        ImapSettingScreenUiState(
            textInputEvents = immutableListOf(),
            loadingState = ImapSettingScreenUiState.LoadingState.Loading,
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {
                    navController.navigateToHome()
                }
            },
            event = object : ImapSettingScreenUiState.Event {
                override fun consumeTextInputEvent(event: ImapSettingScreenUiState.TextInputUiState) {
                    viewModelStateFlow.update {
                        it.copy(
                            textInputEvents = it.textInputEvents.minus(event),
                        )
                    }
                }

                override fun onResume() {
                    load()
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow
                .collect { viewModelState ->
                    val imapConfig = viewModelState.imapConfig

                    val loadingState = if (imapConfig == null) {
                        ImapSettingScreenUiState.LoadingState.Loading
                    } else {
                        ImapSettingScreenUiState.LoadingState.Loaded(
                            imapConfig = ImapSettingScreenUiState.ImapConfig(
                                host = imapConfig.host.orEmpty(),
                                port = imapConfig.port?.toString().orEmpty(),
                                userName = imapConfig.userName.orEmpty(),
                                password = if (imapConfig.hasPassword == true) {
                                    "****************"
                                } else {
                                    ""
                                },
                                event = imapConfigEvent,
                            ),
                            geminiConfig = ImapSettingScreenUiState.GeminiConfig(
                                apiKey = if (viewModelState.hasGeminiApiKey) {
                                    "****************"
                                } else {
                                    ""
                                },
                                event = geminiConfigEvent,
                            ),
                        )
                    }

                    println("loadingState: $loadingState")
                    uiStateFlow.update {
                        it.copy(
                            textInputEvents = viewModelState.textInputEvents.toImmutableList(),
                            loadingState = loadingState,
                        )
                    }
                }
        }
    }.asStateFlow()

    private val geminiConfigEvent = object : ImapSettingScreenUiState.GeminiConfig.Event {
        override fun onClickChangeApiKey() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    textInputEvents = viewModelState.textInputEvents.plus(
                        createTextInputEvent(
                            title = "Gemini API Key",
                            inputType = TextFieldType.Password,
                            default = null,
                            complete = { text, event ->
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setGeminiApiKey(
                                            apiKey = text,
                                        )
                                    }
                                }.onFailure {
                                    Logger.e(TAG, it)
                                }.getOrNull()
                                val geminiConfig = result?.data?.userMutation?.settingsMutation?.updateGeminiApiKey
                                if (geminiConfig == null) {
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                    return@createTextInputEvent
                                }

                                viewModelStateFlow.update {
                                    it.copy(
                                        hasGeminiApiKey = geminiConfig.hasApiKey,
                                        textInputEvents = it.textInputEvents.minus(event),
                                    )
                                }
                            },
                        ),
                    ),
                )
            }
        }
    }

    private val imapConfigEvent = object : ImapSettingScreenUiState.ImapConfig.Event {
        override fun onClickChangeHost() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    textInputEvents = viewModelState.textInputEvents.plus(
                        createTextInputEvent(
                            title = "ホスト名",
                            inputType = TextFieldType.Text,
                            default = viewModelState.imapConfig?.host,
                            complete = { text, event ->
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setImapHost(
                                            host = text,
                                        )
                                    }
                                }.onFailure {
                                    Logger.e(TAG, it)
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                    return@createTextInputEvent
                                }.getOrNull() ?: return@createTextInputEvent

                                val updateImapConfig = result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
                                    ?: return@createTextInputEvent

                                viewModelStateFlow.update {
                                    it.copy(
                                        imapConfig = updateImapConfig,
                                        textInputEvents = it.textInputEvents.minus(event),
                                    )
                                }
                            },
                        ),
                    ),
                )
            }
        }

        override fun onClickChangeUserName() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    textInputEvents = viewModelState.textInputEvents.plus(
                        createTextInputEvent(
                            title = "ユーザー名",
                            inputType = TextFieldType.Text,
                            default = viewModelState.imapConfig?.userName,
                            complete = { text, event ->
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setImapUserName(
                                            userName = text,
                                        )
                                    }
                                }.onFailure {
                                    Logger.e(TAG, it)
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                    return@createTextInputEvent
                                }.getOrNull() ?: return@createTextInputEvent

                                val updateImapConfig = result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
                                    ?: return@createTextInputEvent

                                viewModelStateFlow.update {
                                    it.copy(
                                        imapConfig = updateImapConfig,
                                        textInputEvents = it.textInputEvents.minus(event),
                                    )
                                }
                            },
                        ),
                    ),
                )
            }
        }

        override fun onClickChangePort() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    textInputEvents = viewModelState.textInputEvents.plus(
                        createTextInputEvent(
                            title = "ポート",
                            inputType = TextFieldType.Text,
                            default = viewModelState.imapConfig?.port?.toString(),
                            complete = { text, event ->
                                val port = text.toIntOrNull()
                                if (port == null) {
                                    globalEventSender.send {
                                        it.showNativeNotification("数値を入力してください")
                                    }
                                    return@createTextInputEvent
                                }
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setImapPort(
                                            port = port,
                                        )
                                    }
                                }.onFailure {
                                    Logger.e(TAG, it)
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                    return@createTextInputEvent
                                }.getOrNull() ?: return@createTextInputEvent

                                val updateImapConfig = result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
                                    ?: return@createTextInputEvent

                                viewModelStateFlow.update {
                                    it.copy(
                                        imapConfig = updateImapConfig,
                                        textInputEvents = it.textInputEvents.minus(event),
                                    )
                                }
                            },
                        ),
                    ),
                )
            }
        }

        override fun onClickChangePassword() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    textInputEvents = viewModelState.textInputEvents.plus(
                        createTextInputEvent(
                            title = "パスワード",
                            inputType = TextFieldType.Password,
                            default = null,
                            complete = { text, event ->
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setImapPassword(
                                            password = text,
                                        )
                                    }
                                }.onFailure {
                                    Logger.e(TAG, it)
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                }.getOrNull() ?: return@createTextInputEvent
                                val updateImapConfig = result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
                                    ?: return@createTextInputEvent

                                viewModelStateFlow.update {
                                    it.copy(
                                        imapConfig = updateImapConfig,
                                        textInputEvents = it.textInputEvents.minus(event),
                                    )
                                }
                            },
                        ),
                    ),
                )
            }
        }
    }

    private fun createTextInputEvent(
        title: String,
        default: String?,
        inputType: TextFieldType,
        complete: suspend (text: String, event: ImapSettingScreenUiState.TextInputUiState) -> Unit,
    ): ImapSettingScreenUiState.TextInputUiState {
        return ImapSettingScreenUiState.TextInputUiState(
            title = title,
            default = default.orEmpty(),
            inputType = inputType,
            event = object : ImapSettingScreenUiState.TextInputUiState.Event {
                override fun complete(
                    text: String,
                    event: ImapSettingScreenUiState.TextInputUiState,
                ) {
                    viewModelScope.launch {
                        complete(text, event)
                    }
                }

                override fun cancel(event: ImapSettingScreenUiState.TextInputUiState) {
                    viewModelStateFlow.update {
                        it.copy(
                            textInputEvents = it.textInputEvents.minus(event),
                        )
                    }
                }
            },
        )
    }

    private fun load() {
        viewModelScope.launch {
            val configFLow = withContext(ioDispatchers) {
                runCatching {
                    graphqlQuery.getConfig()
                }.onFailure {
                    Logger.e(TAG, it)
                }.getOrNull()
            } ?: return@launch
            val settings = configFLow.data?.user?.settings

            viewModelStateFlow.update {
                it.copy(
                    imapConfig = settings?.imapConfig?.displayImapConfig,
                    hasGeminiApiKey = settings?.geminiConfig?.hasApiKey == true,
                )
            }
        }
    }

    private data class ViewModelState(
        val imapConfig: DisplayImapConfig? = null,
        val hasGeminiApiKey: Boolean = false,
        val textInputEvents: List<ImapSettingScreenUiState.TextInputUiState> = listOf(),
    )
}
