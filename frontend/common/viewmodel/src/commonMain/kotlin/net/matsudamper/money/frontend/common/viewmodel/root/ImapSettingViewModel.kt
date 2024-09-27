package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.ui.screen.root.settings.ImapSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.ViewModelFeature
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.graphql.GraphqlUserConfigQuery
import net.matsudamper.money.frontend.graphql.fragment.DisplayImapConfig

public class ImapSettingViewModel(
    viewModelFeature: ViewModelFeature,
    private val graphqlQuery: GraphqlUserConfigQuery,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val ioDispatchers: CoroutineDispatcher,
) : CommonViewModel(viewModelFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiState: StateFlow<ImapSettingScreenUiState> =
        MutableStateFlow(
            ImapSettingScreenUiState(
                textInputEvents = immutableListOf(),
                loadingState = ImapSettingScreenUiState.LoadingState.Loading,
                event =
                object : ImapSettingScreenUiState.Event {
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

                        val loadingState =
                            if (imapConfig == null) {
                                ImapSettingScreenUiState.LoadingState.Loading
                            } else {
                                ImapSettingScreenUiState.LoadingState.Loaded(
                                    imapConfig =
                                    ImapSettingScreenUiState.ImapConfig(
                                        host = imapConfig.host.orEmpty(),
                                        port = imapConfig.port?.toString().orEmpty(),
                                        userName = imapConfig.userName.orEmpty(),
                                        password =
                                        if (imapConfig.hasPassword == true) {
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

    private val imapConfigEvent =
        object : ImapSettingScreenUiState.ImapConfig.Event {
            override fun onClickChangeHost() {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        textInputEvents =
                        viewModelState.textInputEvents.plus(
                            createEvent(
                                title = "ホスト名",
                                default = viewModelState.imapConfig?.host,
                                complete = { text, event ->
                                    val result =
                                        runCatching {
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

                                    val updateImapConfig =
                                        result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
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
                        textInputEvents =
                        viewModelState.textInputEvents.plus(
                            createEvent(
                                title = "ユーザー名",
                                default = viewModelState.imapConfig?.userName,
                                complete = { text, event ->
                                    val result =
                                        runCatching {
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

                                    val updateImapConfig =
                                        result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
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
                        textInputEvents =
                        viewModelState.textInputEvents.plus(
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
                                    val result =
                                        runCatching {
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

                                    val updateImapConfig =
                                        result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
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
                        textInputEvents =
                        viewModelState.textInputEvents.plus(
                            createEvent(
                                title = "パスワード",
                                default = null,
                                complete = { text, event ->
                                    val result =
                                        runCatching {
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
                                    val updateImapConfig =
                                        result.data?.userMutation?.settingsMutation?.updateImapConfig?.displayImapConfig
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
                complete: suspend (text: String, event: ImapSettingScreenUiState.TextInputUiState) -> Unit,
            ): ImapSettingScreenUiState.TextInputUiState {
                return ImapSettingScreenUiState.TextInputUiState(
                    title = title,
                    default = default.orEmpty(),
                    event =
                    object : ImapSettingScreenUiState.TextInputUiState.Event {
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
        }

    private fun load() {
        println("load")
        viewModelScope.launch {
            val configFLow =
                withContext(ioDispatchers) {
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
        val textInputEvents: List<ImapSettingScreenUiState.TextInputUiState> = listOf(),
    )
}
