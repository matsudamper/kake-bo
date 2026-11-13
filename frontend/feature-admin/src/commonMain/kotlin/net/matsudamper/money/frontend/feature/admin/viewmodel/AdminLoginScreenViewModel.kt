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
import net.matsudamper.money.frontend.graphql.GraphqlUserLoginQuery
import net.matsudamper.money.frontend.graphql.type.UserFidoLoginInput

private const val TAG = "AdminLoginScreenViewModel"

internal class AdminLoginScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
    private val userLoginQuery: GraphqlUserLoginQuery,
    private val navController: ScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val webAuthModel: WebAuthModel,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : AdminLoginScreenUiState.Listener {
        override fun onClickUserAccount() {
            viewModelStateFlow.update { state -> state.copy(step = Step.UserAccount) }
        }

        override fun onClickAdminAccount() {
            viewModelStateFlow.update { state -> state.copy(step = Step.AdminAccount) }
        }

        override fun onUserNameChanged(text: String) {
            viewModelStateFlow.update { state -> state.copy(userName = TextFieldValue(text)) }
        }

        override fun onUserPasswordChanged(text: String) {
            viewModelStateFlow.update { state -> state.copy(userPassword = TextFieldValue(text)) }
        }

        override fun onClickUserLogin() {
            val state = viewModelStateFlow.value
            loginWithUserPassword(
                userName = state.userName.text,
                password = state.userPassword.text,
            )
        }

        override fun onClickUserSecurityKeyLogin() {
            loginWithUserFido(WebAuthModel.WebAuthModelType.CROSS_PLATFORM)
        }

        override fun onClickUserDeviceKeyLogin() {
            loginWithUserFido(WebAuthModel.WebAuthModelType.PLATFORM)
        }

        override fun onAdminPasswordChanged(text: String) {
            viewModelStateFlow.update { state -> state.copy(adminPassword = TextFieldValue(text)) }
        }

        override fun onClickAdminLogin() {
            login(viewModelStateFlow.value.adminPassword.text)
        }

        override fun onClickBackToSelectAccount() {
            viewModelStateFlow.update { state -> state.copy(step = Step.SelectAccount) }
        }
    }

    public val uiStateFlow: StateFlow<AdminLoginScreenUiState> = MutableStateFlow(
        createUiState(ViewModelState()),
    ).also { uiStateFlow ->
        viewModelScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { createUiState(viewModelState) }
            }
        }
    }.asStateFlow()

    init {
        viewModelScope.launch {
            val isPasswordLoginEnabled = runCatching { adminQuery.getLoginScreen() }
                .onFailure { Logger.e(TAG, it) }
                .getOrNull()
                ?.data?.isAdminPasswordLoginEnabled
            // 取得に失敗しただけで管理アカウントを選べなくすると、ユーザーセッションを持たない管理者が入れなくなる
            if (isPasswordLoginEnabled == false) {
                viewModelStateFlow.update { it.copy(isPasswordLoginDisabled = true) }
            }
        }
    }

    private fun createUiState(viewModelState: ViewModelState): AdminLoginScreenUiState {
        return AdminLoginScreenUiState(
            content = when (viewModelState.step) {
                Step.SelectAccount -> AdminLoginScreenUiState.Content.SelectAccount(
                    adminAccountButtonEnabled = !viewModelState.isPasswordLoginDisabled,
                )

                Step.UserAccount -> AdminLoginScreenUiState.Content.UserAccount(
                    userName = viewModelState.userName,
                    password = viewModelState.userPassword,
                )

                Step.AdminAccount -> AdminLoginScreenUiState.Content.AdminAccount(
                    password = viewModelState.adminPassword,
                )
            },
            listener = listener,
        )
    }

    private fun login(password: String) {
        // TODO: validate  "!@#$%^&*()_+-?<>,."
        viewModelScope.launch {
            val result = runCatching {
                adminQuery.adminLogin(password)
            }.onFailure {
                Logger.e(TAG, it)
            }.getOrNull()

            val isSuccess = result?.data?.adminMutation?.adminLogin?.isSuccess ?: false
            onLoginResult(isSuccess)
        }
    }

    private fun loginWithUserPassword(
        userName: String,
        password: String,
    ) {
        viewModelScope.launch {
            val isSuccess = runCatching {
                val isUserLoginSuccess = userLoginQuery.login(
                    userName = userName,
                    password = password,
                ).data?.userMutation?.userLogin?.isSuccess == true
                isUserLoginSuccess && elevateUserSessionToAdmin()
            }
                .onFailure { Logger.e(TAG, it) }
                .getOrNull()
                ?: false
            onLoginResult(isSuccess)
        }
    }

    private fun loginWithUserFido(type: WebAuthModel.WebAuthModelType) {
        viewModelScope.launch {
            val isSuccess = runCatching {
                val fidoInfo = adminQuery.getFidoLoginInfo().data?.fidoLoginInfo
                    ?: return@runCatching false
                val webAuthResult = webAuthModel.get(
                    type = type,
                    challenge = fidoInfo.challenge,
                    domain = fidoInfo.domain,
                ) ?: return@runCatching false
                val isUserLoginSuccess = userLoginQuery.webAuthLogin(
                    input = UserFidoLoginInput(
                        base64AuthenticatorData = webAuthResult.base64AuthenticatorData,
                        base64ClientDataJson = webAuthResult.base64ClientDataJSON,
                        base64Signature = webAuthResult.base64Signature,
                        base64UserHandle = webAuthResult.base64UserHandle,
                        credentialId = webAuthResult.credentialId,
                        userName = viewModelStateFlow.value.userName.text,
                        challenge = fidoInfo.challenge,
                    ),
                ).data?.userMutation?.userFidoLogin?.isSuccess == true
                isUserLoginSuccess && elevateUserSessionToAdmin()
            }
                .onFailure { Logger.e(TAG, it) }
                .getOrNull()
                ?: false
            onLoginResult(isSuccess)
        }
    }

    private suspend fun elevateUserSessionToAdmin(): Boolean {
        return adminQuery.adminLoginWithUserSession()
            .data?.adminMutation?.adminLoginWithUserSession?.isSuccess == true
    }

    private suspend fun onLoginResult(isSuccess: Boolean) {
        if (isSuccess) {
            navController.navigateReplace(ScreenStructure.Admin.Root)
        } else {
            globalEventSender.send { it.showSnackBar("ログインに失敗しました") }
        }
    }

    private enum class Step {
        SelectAccount,
        UserAccount,
        AdminAccount,
    }

    private data class ViewModelState(
        val isPasswordLoginDisabled: Boolean = false,
        val step: Step = Step.SelectAccount,
        val userName: TextFieldValue = TextFieldValue(),
        val userPassword: TextFieldValue = TextFieldValue(),
        val adminPassword: TextFieldValue = TextFieldValue(),
    )
}
