package net.matsudamper.money.frontend.common.viewmodel.root.settings.login

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.ImmutableList.Companion.toImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.navigator.WebAuthModel
import net.matsudamper.money.frontend.common.ui.screen.root.settings.LoginSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EqualsImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.shared.FidoApi
import net.matsudamper.money.frontend.graphql.LoginSettingScreenQuery

public class LoginSettingViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: LoginSettingScreenApi,
    private val fidoApi: FidoApi,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(
            apolloScreenResponse = null,
            textInputDialogState = null,
        ),
    )
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiStateFlow: StateFlow<LoginSettingScreenUiState> = MutableStateFlow(
        LoginSettingScreenUiState(
            fidoList = immutableListOf(),
            textInputDialogState = null,
            event = object : LoginSettingScreenUiState.Event {
                override fun onClickBack() {
                    coroutineScope.launch {
                        eventSender.send { it.navigate(ScreenStructure.Root.Settings.Root) }
                    }
                }

                override fun onClickPlatform() {
                    createFido(WebAuthModel.Type.PLATFORM)
                }

                override fun onClickCrossPlatform() {
                    createFido(WebAuthModel.Type.CROSS_PLATFORM)
                }

                override fun onClickLogout() {
                    coroutineScope.launch {
                        val result = api.logout()
                        if (result) {
                            eventSender.send { it.navigate(ScreenStructure.Login) }
                        } else {
                            eventSender.send { it.showToast("ログアウトに失敗しました") }
                        }
                    }
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        fidoList = run fidoList@{
                            val fidoList = viewModelState.apolloScreenResponse
                                ?.data?.user?.settings?.registeredFidoList
                            if (fidoList == null) {
                                return@fidoList immutableListOf()
                            }

                            fidoList.map { fido ->
                                LoginSettingScreenUiState.Fido(
                                    name = fido.name,
                                    event = FidoEventImpl(fido),
                                )
                            }.toImmutableList()
                        },
                        textInputDialogState = viewModelState.textInputDialogState,
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            api.getScreen().collectLatest { apolloResponse ->
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        apolloScreenResponse = apolloResponse,
                    )
                }
            }
        }
    }

    private fun createFido(type: WebAuthModel.Type) {
        coroutineScope.launch {
            val fidoInfo = fidoApi.getFidoInfo()
                .getOrNull()?.data?.user?.settings?.fidoAddInfo
            if (fidoInfo == null) {
                showAddFidoFailToast()
                return@launch
            }

            val createResult = WebAuthModel.create(
                id = fidoInfo.id,
                name = fidoInfo.name,
                type = type,
                challenge = fidoInfo.challenge,
                domain = fidoInfo.domain,
                base64ExcludeCredentialIdList = viewModelStateFlow.value.apolloScreenResponse?.data?.user?.settings?.registeredFidoList.orEmpty().map {
                    it.base64CredentialId
                },
            )

            if (createResult == null) {
                showAddFidoFailToast()
                return@launch
            }
            val onConfirm: (String) -> Unit = { name ->
                coroutineScope.launch onConfirm@{
                    if (name.isBlank()) {
                        eventSender.send { it.showToast("入力してください") }
                        return@onConfirm
                    }
                    val result = withContext(Dispatchers.Default) {
                        api.addFido(
                            displayName = name,
                            base64AttestationObject = createResult.attestationObjectBase64,
                            base64ClientDataJson = createResult.clientDataJSONBase64,
                        )
                    }

                    val registerFidoResult = result?.data?.userMutation?.registerFido
                    if (registerFidoResult?.fidoInfo == null) {
                        showAddFidoFailToast()
                    } else {
                        eventSender.send { it.showToast("追加しました") }
                        // TODO update list
                    }
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            textInputDialogState = null,
                        )
                    }
                }
            }
            val onCancel: () -> Unit = {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(
                        textInputDialogState = null,
                    )
                }
            }
            viewModelStateFlow.update { viewModelState ->
                viewModelState.copy(
                    textInputDialogState = LoginSettingScreenUiState.TextInputDialogState(
                        title = "キーの名前を入力してください",
                        text = "",
                        onConfirm = onConfirm,
                        onCancel = onCancel,
                        type = "text",
                    ),
                )
            }
        }
    }

    private fun createFidoTextInputDialogState(): LoginSettingScreenUiState.TextInputDialogState {
        val onConfirm: (String) -> Unit = {}
        val onCancel: () -> Unit = {}
        return LoginSettingScreenUiState.TextInputDialogState(
            title = "キーの名前を入力してください",
            text = "",
            onConfirm = onConfirm,
            onCancel = onCancel,
            type = "text",
        )
    }

    private fun showAddFidoFailToast() {
        coroutineScope.launch {
            eventSender.send { it.showToast("追加に失敗しました") }
        }
    }

    private class FidoEventImpl(
        private val item: LoginSettingScreenQuery.RegisteredFidoList,
    ) : LoginSettingScreenUiState.Fido.Event, EqualsImpl(item) {
        override fun onClickDelete() {
            TODO(item.toString())
        }
    }

    private data class ViewModelState(
        val apolloScreenResponse: ApolloResponse<LoginSettingScreenQuery.Data>?,
        val textInputDialogState: LoginSettingScreenUiState.TextInputDialogState?,
    )

    public interface Event {
        public fun showToast(text: String)
        public fun navigate(structure: ScreenStructure)
    }
}
