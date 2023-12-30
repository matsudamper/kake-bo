package net.matsudamper.money.frontend.common.viewmodel.root.settings.login

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.navigator.WebAuthModel
import net.matsudamper.money.frontend.common.ui.screen.root.settings.LoginSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.shared.FidoApi

public class LoginSettingViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: LoginSettingScreenApi,
    private val fidoApi: FidoApi,
) {
    private val eventSender = EventSender<Event>()
    public val eventHandler: EventHandler<Event> = eventSender.asHandler()

    public val uiStateFlow: StateFlow<LoginSettingScreenUiState> = MutableStateFlow(
        LoginSettingScreenUiState(
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
    ).also {
    }.asStateFlow()

    private fun createFido(type: WebAuthModel.Type) {
        coroutineScope.launch {
            val fidoInfo = fidoApi.getFidoInfo()
                .getOrNull()?.data?.fidoInfo
            if (fidoInfo == null) {
                showAddFidoFailToast()
                return@launch
            }

            val createResult = WebAuthModel.create(
                userId = 1,
                name = "test",
                type = type,
                challenge = fidoInfo.challenge,
                domain = fidoInfo.domain,
            )

            if (createResult == null) {
                showAddFidoFailToast()
                return@launch
            }

            val result = withContext(Dispatchers.Default) {
                api.addFido(
                    base64AttestationObject = createResult.attestationObjectBase64,
                    base64ClientDataJson = createResult.clientDataJSONBase64,
                )
            }

            val registerFidoResult = result?.data?.userMutation?.registerFido
            if (registerFidoResult == null || registerFidoResult.not()) {
                showAddFidoFailToast()
            } else {
                eventSender.send { it.showToast("追加しました") }
                // TODO update list
            }
        }
    }

    private fun showAddFidoFailToast() {
        coroutineScope.launch {
            eventSender.send { it.showToast("追加に失敗しました") }
        }
    }

    public interface Event {
        public fun showToast(text: String)
        public fun navigate(structure: ScreenStructure)
    }
}
