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
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery

public class LoginScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlQuery: GraphqlUserLoginQuery,
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
                override fun onPasswordChange(value: TextFieldValue) {
                    viewModelStateFlow.update {
                        it.copy(password = value)
                    }
                }

                override fun onUserNameChange(value: TextFieldValue) {
                    viewModelStateFlow.update {
                        it.copy(userName = value)
                    }
                }

                override fun onClickLogin() {
                    coroutineScope.launch {
                        val result = runCatching {
                            graphqlQuery.login(
                                userName = viewModelStateFlow.value.userName.text,
                                password = viewModelStateFlow.value.password.text,
                            )
                        }.getOrNull()

                        if (result?.data?.userMutation?.userLogin?.isSuccess == true) {
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
                }

                override fun onClickDeviceKeyLogin() {
                }
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        userName = viewModelState.userName,
                        password = viewModelState.password,
                        textInputDialog = viewModelState.textInputDialogUiState,
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            val isLoggedIn = graphqlQuery.isLoggedIn()
            if (isLoggedIn) {
                navController.navigate(RootHomeScreenStructure.Home)
            }
        }
    }

    private data class ViewModelState(
        val userName: TextFieldValue = TextFieldValue(),
        val password: TextFieldValue = TextFieldValue(),
        val textInputDialogUiState: LoginScreenUiState.TextInputDialogUiState? = null,
    )
    /**
     * TODO FIDO Login implementation
     * console.log(createResult)
     *             createResult ?: return@launch
     *             val getResult = CredentialModel.get(
     *                 userId = 1,
     *                 name = "test",
     *                 type = type,
     *                 challenge = "test", // TODO challenge
     *                 domain = "domain",
     *             ) ?: return@launch
     *             console.log(getResult)
     */
}
