package net.matsudamper.money.frontend.common.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.feature.webauth.WebAuthModel
import net.matsudamper.money.frontend.common.ui.screen.login.LoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.login.LoginScreenApi
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.graphql.ServerHostConfig
import net.matsudamper.money.frontend.graphql.type.UserFidoLoginInput

public class LoginScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val graphqlQuery: GraphqlUserLoginQuery,
    private val screenApi: LoginScreenApi,
    private val navController: ScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val webAuthModel: WebAuthModel,
    private val graphqlClient: GraphqlClient,
    private val serverHostConfig: ServerHostConfig?,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow: MutableStateFlow<ViewModelState> = MutableStateFlow(
        run {
            val initialHost = serverHostConfig?.savedHost.orEmpty()
            ViewModelState(
                selectedHost = initialHost,
                customHosts = buildList {
                    if (serverHostConfig != null) {
                        val savedHost = serverHostConfig.savedHost
                        if (savedHost.isNotEmpty() && savedHost != serverHostConfig.defaultHost) {
                            add(savedHost)
                        }
                    }
                },
            )
        },
    )
    public val uiStateFlow: StateFlow<LoginScreenUiState> = MutableStateFlow(
        LoginScreenUiState(
            userName = TextFieldValue(),
            password = TextFieldValue(),
            serverHost = null,
            listener = object : LoginScreenUiState.Listener {
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

                override fun onSelectServerHost(host: String) {
                    viewModelStateFlow.update { it.copy(selectedHost = host) }
                    if (serverHostConfig != null) {
                        graphqlClient.updateServerUrl("${serverHostConfig.protocol}://$host/query")
                    }
                }

                override fun onClickAddCustomHost() {
                    viewModelStateFlow.update { it.copy(customHostDialogText = "") }
                }

                override fun onCustomHostTextChanged(text: String) {
                    viewModelStateFlow.update { it.copy(customHostDialogText = text) }
                }

                override fun onConfirmCustomHost() {
                    val text = viewModelStateFlow.value.customHostDialogText.orEmpty().trim()
                    if (text.isEmpty()) return
                    viewModelStateFlow.update {
                        val updatedCustomHosts = if (it.customHosts.contains(text)) {
                            it.customHosts
                        } else {
                            it.customHosts + text
                        }
                        it.copy(
                            selectedHost = text,
                            customHosts = updatedCustomHosts,
                            customHostDialogText = null,
                        )
                    }
                    if (serverHostConfig != null) {
                        graphqlClient.updateServerUrl("${serverHostConfig.protocol}://$text/query")
                    }
                }

                override fun onDismissCustomHostDialog() {
                    viewModelStateFlow.update { it.copy(customHostDialogText = null) }
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
                        serverHost = createServerHostUiState(viewModelState),
                    )
                }
            }
        }
    }.asStateFlow()

    private fun createServerHostUiState(state: ViewModelState): LoginScreenUiState.ServerHostUiState? {
        if (serverHostConfig == null) return null
        val hosts = buildList {
            val defaultHost = serverHostConfig.defaultHost
            if (defaultHost.isNotEmpty()) {
                add(defaultHost)
            }
            for (customHost in state.customHosts) {
                if (customHost != defaultHost) {
                    add(customHost)
                }
            }
        }
        return LoginScreenUiState.ServerHostUiState(
            selectedHost = state.selectedHost,
            hosts = hosts,
            customHostDialogText = state.customHostDialogText,
        )
    }

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
            val fidoInfo = screenApi.fidoLoginInfo()
                .data?.fidoLoginInfo

            if (fidoInfo == null) {
                globalEventSender.send {
                    it.showSnackBar("ログインに失敗しました")
                }
                return@launch
            }

            val webAuthResult = webAuthModel.get(
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
        val selectedHost: String = "",
        val customHosts: List<String> = listOf(),
        val customHostDialogText: String? = null,
    )
}
