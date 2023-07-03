package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery
import net.matsudamper.money.frontend.graphql.fragment.DisplayImapConfig
import net.matsudamper.root.RootSettingScreenUiState

public class SettingViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlQuery: GraphqlUserConfigQuery,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val ioDispatchers: CoroutineDispatcher,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiState: StateFlow<RootSettingScreenUiState> = MutableStateFlow(
        RootSettingScreenUiState(
            textInputEvents = immutableListOf(),
            loadingState = RootSettingScreenUiState.LoadingState.Loading,
            event = object : RootSettingScreenUiState.Event {
                override fun consumeTextInputEvent(event: RootSettingScreenUiState.TextInputUiState) {
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
        coroutineScope.launch {
            viewModelStateFlow
                .collect { viewModelState ->
                    val imapConfig = viewModelState.imapConfig

                    val loadingState = if (imapConfig == null) {
                        RootSettingScreenUiState.LoadingState.Loading
                    } else {
                        RootSettingScreenUiState.LoadingState.Loaded(
                            imapConfig = RootSettingScreenUiState.ImapConfig(
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

    private val imapConfigEvent = object : RootSettingScreenUiState.ImapConfig.Event {
        override fun onClickChangeHost() {
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    textInputEvents = viewModelState.textInputEvents.plus(
                        createEvent(
                            title = "ホスト名",
                            default = viewModelState.imapConfig?.host,
                            complete = { text, event ->
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setImapHost(
                                            host = text,
                                        )
                                    }
                                }.onFailure {
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                    return@createEvent
                                }.getOrNull() ?: return@createEvent

                                val updateImapConfig = result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
                                    ?: return@createEvent

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
                        createEvent(
                            title = "ユーザー名",
                            default = viewModelState.imapConfig?.userName,
                            complete = { text, event ->
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setImapUserName(
                                            userName = text,
                                        )
                                    }
                                }.onFailure {
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                    return@createEvent
                                }.getOrNull() ?: return@createEvent

                                val updateImapConfig = result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
                                    ?: return@createEvent

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
                        createEvent(
                            title = "ポート",
                            default = viewModelState.imapConfig?.port?.toString(),
                            complete = { text, event ->
                                val port = text.toIntOrNull()
                                if (port == null) {
                                    globalEventSender.send {
                                        it.showNativeNotification("数値を入力してください")
                                    }
                                    return@createEvent
                                }
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setImapPort(
                                            port = port,
                                        )
                                    }
                                }.onFailure {
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                    return@createEvent
                                }.getOrNull() ?: return@createEvent

                                val updateImapConfig = result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
                                    ?: return@createEvent

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
                        createEvent(
                            title = "パスワード",
                            default = null,
                            complete = { text, event ->
                                val result = runCatching {
                                    withContext(ioDispatchers) {
                                        graphqlQuery.setImapPassword(
                                            password = text,
                                        )
                                    }
                                }.onFailure {
                                    globalEventSender.send {
                                        it.showNativeNotification("更新に失敗しました")
                                    }
                                }.getOrNull() ?: return@createEvent
                                val updateImapConfig = result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
                                    ?: return@createEvent

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

        private fun createEvent(
            title: String,
            default: String?,
            complete: suspend (text: String, event: RootSettingScreenUiState.TextInputUiState) -> Unit,
        ): RootSettingScreenUiState.TextInputUiState {
            return RootSettingScreenUiState.TextInputUiState(
                title = title,
                default = default.orEmpty(),
                event = object : RootSettingScreenUiState.TextInputUiState.Event {
                    override fun complete(text: String, event: RootSettingScreenUiState.TextInputUiState) {
                        coroutineScope.launch {
                            complete(text, event)
                        }
                    }

                    override fun cancel(event: RootSettingScreenUiState.TextInputUiState) {
                        viewModelStateFlow.update {
                            it.copy(
                                textInputEvents = it.textInputEvents.minus(event),
                            )
                        }
                    }
                },
            )
        }
    }

    private fun load() {
        println("load")
        coroutineScope.launch {
            val configFLow = withContext(ioDispatchers) {
                runCatching {
                    graphqlQuery.getConfig()
                }.onFailure {

                }.getOrNull()
            } ?: return@launch
            val displayImapConfig = configFLow.data?.user?.settings?.imapConfig?.displayImapConfig

            viewModelStateFlow.update {
                it.copy(
                    imapConfig = displayImapConfig,
                )
            }
        }
    }


    private data class ViewModelState(
        val imapConfig: DisplayImapConfig? = null,
        val textInputEvents: List<RootSettingScreenUiState.TextInputUiState> = listOf(),
    )
}