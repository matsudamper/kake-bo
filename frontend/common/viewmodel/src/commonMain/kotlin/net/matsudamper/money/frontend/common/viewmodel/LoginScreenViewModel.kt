package net.matsudamper.money.frontend.common.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.JsScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModel
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.login.LoginScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.graphql.type.UserFidoLoginInput

public class LoginScreenViewModel(
    viewModelFeature: ViewModelFeature,
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val screenApi: LoginScreenApi,
    private val navController: JsScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val webAuthModel: WebAuthModel,
) : CommonViewModel(viewModelFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> =
        MutableStateFlow(
            ViewModelState(),
        )
    public val uiStateFlow: StateFlow<LoginScreenUiState> =
        MutableStateFlow(
            LoginScreenUiState(
                userName = TextFieldValue(),
                password = TextFieldValue(),
                listener =
                object : LoginScreenUiState.Listener {
                    override fun onPasswordChanged(text: String) {
                        viewModelStateFlow.update {
                            it.copy(password = TextFieldValue(text))
                        }
                    }

                    override fun onUserIdChanged(text: String) {
                        viewModelStateFlow.update {
                            it.copy(userName = TextFieldValue(text))
                        }
                    }

                    override fun onClickLogin() {
                        viewModelScope.launch {
                            val result = runCatching {
                                graphqlQuery.login(
                                    userName = viewModelStateFlow.value.userName.text,
                                    password = viewModelStateFlow.value.password.text,
                                )
                            }.getOrNull()
                            postLogin(isSuccess = result?.data?.userMutation?.userLogin?.isSuccess == true)
                        }
                    }

                    override fun onClickNavigateAdmin() {
                        navController.navigate(ScreenStructure.Admin)
                    }

                    override fun onClickSecurityKeyLogin() {
                        login(
                            userName = viewModelStateFlow.value.userName.text,
                            type = WebAuthModel.WebAuthModelType.CROSS_PLATFORM,
                        )
                    }

                    override fun onClickDeviceKeyLogin() {
                        login(
                            userName = viewModelStateFlow.value.userName.text,
                            type = WebAuthModel.WebAuthModelType.PLATFORM,
                        )
                    }
                },
            ),
        ).also { uiStateFlow ->
            viewModelScope.launch {
                viewModelStateFlow.collect { viewModelState ->
                    uiStateFlow.update { uiState ->
                        uiState.copy(
                            userName = viewModelState.userName,
                            password = viewModelState.password,
                        )
                    }
                }
            }
        }.asStateFlow()

    private suspend fun postLogin(isSuccess: Boolean) {
        if (isSuccess) {
            navController.navigate(RootHomeScreenStructure.Home)
            globalEventSender.send {
                it.showSnackBar("ログインしました")
            }
        } else {
            globalEventSender.send {
                it.showSnackBar("ログインに失敗しました")
            }
        }
    }

    init {
        viewModelScope.launch {
            val isLoggedIn = graphqlQuery.isLoggedIn()
            if (isLoggedIn) {
                navController.navigate(RootHomeScreenStructure.Home)
            }
        }
    }

    private fun login(
        userName: String,
        type: WebAuthModel.WebAuthModelType,
    ) {
        viewModelScope.launch {
            val fidoInfo =
                screenApi.fidoLoginInfo()
                    .data?.fidoLoginInfo

            if (fidoInfo == null) {
                globalEventSender.send {
                    it.showSnackBar("ログインに失敗しました")
                }
                return@launch
            }

            val webAuthResult = webAuthModel.get(
                userId = userName,
                name = userName,
                type = type,
                challenge = fidoInfo.challenge,
                domain = fidoInfo.domain,
            )
            if (webAuthResult == null) {
                globalEventSender.send {
                    it.showSnackBar("ログインに失敗しました")
                }
                return@launch
            }
            val loginResult = graphqlQuery.webAuthLogin(
                input = UserFidoLoginInput(
                    base64AuthenticatorData = webAuthResult.base64AuthenticatorData,
                    base64ClientDataJson = webAuthResult.base64ClientDataJSON,
                    base64Signature = webAuthResult.base64Signature,
                    base64UserHandle = webAuthResult.base64UserHandle,
                    credentialId = webAuthResult.credentialId,
                    userName = userName,
                    challenge = fidoInfo.challenge,
                ),
            )
            postLogin(isSuccess = loginResult.data?.userMutation?.userFidoLogin?.isSuccess == true)
        }
    }

    private data class ViewModelState(
        val userName: TextFieldValue = TextFieldValue(),
        val password: TextFieldValue = TextFieldValue(),
    )
}
