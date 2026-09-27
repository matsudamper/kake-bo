package net.matsudamper.money.frontend.feature.admin.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModel
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.feature.admin.ui.AdminLoginScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery
import net.matsudamper.money.frontend.graphql.type.AdminFidoLoginInput

private const val TAG = "AdminLoginScreenViewModel"

internal class AdminLoginScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
    private val navController: ScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val webAuthModel: WebAuthModel,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AdminLoginScreenUiState> = MutableStateFlow(
        AdminLoginScreenUiState(
            password = TextFieldValue(),
            listener = object : AdminLoginScreenUiState.Listener {
                override fun onPasswordChanged(text: String) {
                    viewModelStateFlow.update { state -> state.copy(password = TextFieldValue(text)) }
                }

                override fun onClickLogin() {
                    login(viewModelStateFlow.value.password.text)
                }

                override fun onClickSecurityKeyLogin() {
                    loginWithFido(WebAuthModel.WebAuthModelType.CROSS_PLATFORM)
                }

                override fun onClickDeviceKeyLogin() {
                    loginWithFido(WebAuthModel.WebAuthModelType.PLATFORM)
                }
            },
        ),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        password = viewModelState.password,
                    )
                }
            }
        }
    }.asStateFlow()

    private fun login(password: String) {
        // TODO: validate  "!@#$%^&*()_+-?<>,."
        viewModelScope.launch {
            val result = runCatching {
                adminQuery.adminLogin(password)
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()

            val isSuccess = result?.data?.adminMutation?.adminLogin?.isSuccess ?: false
            postLogin(isSuccess)
        }
    }

    private fun loginWithFido(type: WebAuthModel.WebAuthModelType) {
        viewModelScope.launch {
            val fidoInfo = runCatching {
                adminQuery.fidoLoginInfo()
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()?.data?.fidoLoginInfo

            if (fidoInfo == null) {
                globalEventSender.send { it.showSnackBar("ログインに失敗しました") }
                return@launch
            }

            val webAuthResult = webAuthModel.get(
                type = type,
                challenge = fidoInfo.challenge,
                domain = fidoInfo.domain,
            )
            if (webAuthResult == null) {
                globalEventSender.send { it.showSnackBar("ログインに失敗しました") }
                return@launch
            }

            val loginResult = runCatching {
                adminQuery.adminFidoLogin(
                    input = AdminFidoLoginInput(
                        credentialId = webAuthResult.credentialId,
                        challenge = fidoInfo.challenge,
                        base64AuthenticatorData = webAuthResult.base64AuthenticatorData,
                        base64Signature = webAuthResult.base64Signature,
                        base64ClientDataJson = webAuthResult.base64ClientDataJSON,
                        base64UserHandle = webAuthResult.base64UserHandle,
                    ),
                )
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()

            postLogin(loginResult?.data?.adminMutation?.adminFidoLogin?.isSuccess == true)
        }
    }

    private suspend fun postLogin(isSuccess: Boolean) {
        if (isSuccess) {
            navController.navigateReplace(ScreenStructure.Admin.Root)
        } else {
            globalEventSender.send { it.showSnackBar("ログインに失敗しました") }
        }
    }

    private data class ViewModelState(
        val password: TextFieldValue = TextFieldValue(),
        val isLoggedIn: Boolean = false,
    )
}
