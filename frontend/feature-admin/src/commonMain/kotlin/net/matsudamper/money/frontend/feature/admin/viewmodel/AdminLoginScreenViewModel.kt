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
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.feature.admin.ui.AdminLoginScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

private const val TAG = "AdminLoginScreenViewModel"

internal class AdminLoginScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
    private val navController: ScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    private val listener = object : AdminLoginScreenUiState.Listener {
        override fun onAccountTypeSelected(accountType: AdminLoginScreenUiState.AccountType) {
            viewModelStateFlow.update { state -> state.copy(selectedAccountType = accountType) }
        }

        override fun onPasswordChanged(text: String) {
            viewModelStateFlow.update { state -> state.copy(password = TextFieldValue(text)) }
        }

        override fun onClickLogin() {
            val state = viewModelStateFlow.value
            when (state.resolveAccountType()) {
                AdminLoginScreenUiState.AccountType.Admin -> login(state.password.text)
                AdminLoginScreenUiState.AccountType.User -> loginWithUserSession()
            }
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
        val accountType = viewModelState.resolveAccountType()
        return AdminLoginScreenUiState(
            selectedAccountType = accountType,
            adminAccountOptionEnabled = !viewModelState.isPasswordLoginDisabled,
            passwordInputVisible = accountType == AdminLoginScreenUiState.AccountType.Admin,
            password = viewModelState.password,
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

    private fun loginWithUserSession() {
        viewModelScope.launch {
            val isSuccess = runCatching { adminQuery.adminLoginWithUserSession() }
                .onFailure { Logger.e(TAG, it) }
                .getOrNull()
                ?.data?.adminMutation?.adminLoginWithUserSession?.isSuccess
                ?: false
            onLoginResult(isSuccess)
        }
    }

    private suspend fun onLoginResult(isSuccess: Boolean) {
        if (isSuccess) {
            navController.navigateReplace(ScreenStructure.Admin.Root)
        } else {
            globalEventSender.send { it.showSnackBar("ログインに失敗しました") }
        }
    }

    private data class ViewModelState(
        val selectedAccountType: AdminLoginScreenUiState.AccountType = AdminLoginScreenUiState.AccountType.User,
        val isPasswordLoginDisabled: Boolean = false,
        val password: TextFieldValue = TextFieldValue(),
    ) {
        fun resolveAccountType(): AdminLoginScreenUiState.AccountType {
            return if (isPasswordLoginDisabled) {
                AdminLoginScreenUiState.AccountType.User
            } else {
                selectedAccountType
            }
        }
    }
}
