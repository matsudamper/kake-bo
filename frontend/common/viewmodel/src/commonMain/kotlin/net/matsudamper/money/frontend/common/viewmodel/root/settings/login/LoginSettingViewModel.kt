package net.matsudamper.money.frontend.common.viewmodel.root.settings.login

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.navigator.CredentialModel
import net.matsudamper.money.frontend.common.ui.screen.root.settings.LoginSettingScreenUiState

public class LoginSettingViewModel(
    private val coroutineScope: CoroutineScope,
) {
    public val uiStateFlow: StateFlow<LoginSettingScreenUiState> = MutableStateFlow(
        LoginSettingScreenUiState(
            event = object : LoginSettingScreenUiState.Event {
                override fun onClickBack() {
                }

                override fun onClickPlatform() {
                    createDido(CredentialModel.Type.PLATFORM)
                }

                override fun onClickCrossPlatform() {
                    createDido(CredentialModel.Type.CROSS_PLATFORM)
                }
            },
        ),
    ).also {

    }.asStateFlow()

    private fun createDido(type: CredentialModel.Type) {
        coroutineScope.launch {
            val result = CredentialModel.create(
                userId = 1,
                name = "test",
                type = type,
                challenge = "test",
                domain = "TODO.com",
            )
            console.log(result)
        }
    }
}