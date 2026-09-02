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
            if (isSuccess) {
                navController.navigateReplace(ScreenStructure.Admin.Root)
            } else {
                globalEventSender.send { it.showSnackBar("ログインに失敗しました") }
            }
        }
    }

    private data class ViewModelState(
        val password: TextFieldValue = TextFieldValue(),
        val isLoggedIn: Boolean = false,
    )
}
