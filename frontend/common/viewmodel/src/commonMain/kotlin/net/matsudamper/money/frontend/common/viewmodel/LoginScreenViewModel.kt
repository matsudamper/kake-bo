package net.matsudamper.money.frontend.common.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.ScreenNavController
import net.matsudamper.money.frontend.common.uistate.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent

public class LoginScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val navController: ScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        ViewModelState(),
    )
    public val uiStateFlow: StateFlow<LoginScreenUiState> = MutableStateFlow(
        LoginScreenUiState(
            userName = TextFieldValue(),
            password = TextFieldValue(),
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
                            navController.navigate(Screen.Root.Home)
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
                    navController.navigate(Screen.Admin)
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
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            val isLoggedIn = graphqlQuery.isLoggedIn()
            if (isLoggedIn) {
                navController.navigate(Screen.Root.Home)
            }
        }
    }

    private data class ViewModelState(
        val userName: TextFieldValue = TextFieldValue(),
        val password: TextFieldValue = TextFieldValue(),
    )
}
