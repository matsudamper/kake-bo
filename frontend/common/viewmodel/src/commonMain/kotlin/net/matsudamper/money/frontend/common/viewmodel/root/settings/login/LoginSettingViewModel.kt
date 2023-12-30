package net.matsudamper.money.frontend.common.viewmodel.root.settings.login

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.matsudamper.money.frontend.common.base.navigator.WebAuthModel
import net.matsudamper.money.frontend.common.ui.screen.root.settings.LoginSettingScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.shared.FidoApi

public class LoginSettingViewModel(
    private val coroutineScope: CoroutineScope,
    private val api: LoginSettingScreenApi,
    private val fidoApi: FidoApi,
) {
    public val uiStateFlow: StateFlow<LoginSettingScreenUiState> = MutableStateFlow(
        LoginSettingScreenUiState(
            event = object : LoginSettingScreenUiState.Event {
                override fun onClickBack() {
                }

                override fun onClickPlatform() {
                    createFido(WebAuthModel.Type.PLATFORM)
                }

                override fun onClickCrossPlatform() {
                    createFido(WebAuthModel.Type.CROSS_PLATFORM)
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
                TODO("error")
            }

            val createResult = WebAuthModel.create(
                userId = 1,
                name = "test",
                type = type,
                challenge = fidoInfo.challenge,
                domain = fidoInfo.domain,
            )

            if (createResult == null) {
                TODO("Error")
            }

            withContext(Dispatchers.Default) {
                val result = api.addFido(
                    base64AttestationObject = createResult.attestationObjectBase64,
                    base64ClientDataJson = createResult.clientDataJSONBase64,
                )
                val registerFidoResult = result?.data?.userMutation?.registerFido
                if (registerFidoResult == null || registerFidoResult.not()) {
                    TODO("Error")
                } else {
                    // TODO success notification
                }
            }
        }
    }
}
