package net.matsudamper.money.frontend.common.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.JsScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.base.navigator.WebAuthModel
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.login.LoginScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.graphql.type.UserFidoLoginInput

public class LoginScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val screenApi: LoginScreenApi,
    private val navController: JsScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(),
    )
    public val uiStateFlow: StateFlow<LoginScreenUiState> = MutableStateFlow(
        LoginScreenUiState(
            userName = TextFieldValue(),
            password = TextFieldValue(),
            textInputDialog = null,
            listener = object : LoginScreenUiState.Listener {
                override fun onClickLogin() {
                    coroutineScope.launch {
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

                override fun onClickUserNameTextField() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            textInputDialogUiState = LoginScreenUiState.TextInputDialogUiState(
                                title = "username",
                                name = "username",
                                text = viewModelStateFlow.value.userName.text,
                                inputType = "text",
                                onComplete = { text ->
                                    viewModelStateFlow.update {
                                        it.copy(
                                            userName = TextFieldValue(text),
                                            textInputDialogUiState = null,
                                        )
                                    }
                                },
                                onConfirm = {
                                    viewModelStateFlow.update {
                                        it.copy(textInputDialogUiState = null)
                                    }
                                },
                                onCancel = {
                                    viewModelStateFlow.update {
                                        it.copy(textInputDialogUiState = null)
                                    }
                                },
                            ),
                        )
                    }
                }

                override fun onClickPasswordTextField() {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(
                            textInputDialogUiState = LoginScreenUiState.TextInputDialogUiState(
                                title = "password",
                                name = "password",
                                inputType = "password",
                                text = viewModelStateFlow.value.password.text,
                                onComplete = { text ->
                                    viewModelStateFlow.update {
                                        it.copy(
                                            password = TextFieldValue(text),
                                            textInputDialogUiState = null,
                                        )
                                    }
                                },
                                onConfirm = {
                                    viewModelStateFlow.update {
                                        it.copy(textInputDialogUiState = null)
                                    }
                                },
                                onCancel = {
                                    viewModelStateFlow.update {
                                        it.copy(textInputDialogUiState = null)
                                    }
                                },
                            ),
                        )
                    }
                }

                override fun onClickSecurityKeyLogin() {
                    login(
                        userName = viewModelStateFlow.value.userName.text,
                        type = WebAuthModel.Type.CROSS_PLATFORM,
                    )
                }

                override fun onClickDeviceKeyLogin() {
                    login(
                        userName = viewModelStateFlow.value.userName.text,
                        type = WebAuthModel.Type.PLATFORM,
                    )
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        userName = viewModelState.userName,
                        password = TextFieldValue(
                            "●".repeat(viewModelState.password.text.length),
                        ),
                        textInputDialog = viewModelState.textInputDialogUiState,
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
        coroutineScope.launch {
            val isLoggedIn = graphqlQuery.isLoggedIn()
            if (isLoggedIn) {
                navController.navigate(RootHomeScreenStructure.Home)
            }
        }
    }

    private fun login(
        userName: String,
        type: WebAuthModel.Type,
    ) {
        coroutineScope.launch {
            val fidoInfo = screenApi.fidoLoginInfo()
                .data?.fidoLoginInfo

            if (fidoInfo == null) {
                globalEventSender.send {
                    it.showSnackBar("ログインに失敗しました")
                }
                return@launch
            }

            val webAuthResult = WebAuthModel.get(
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
                ),
            )
            postLogin(isSuccess = loginResult.data?.userMutation?.userFidoLogin?.isSuccess == true)
        }
    }

    private data class ViewModelState(
        val userName: TextFieldValue = TextFieldValue(),
        val password: TextFieldValue = TextFieldValue(),
        val textInputDialogUiState: LoginScreenUiState.TextInputDialogUiState? = null,
    )
}
