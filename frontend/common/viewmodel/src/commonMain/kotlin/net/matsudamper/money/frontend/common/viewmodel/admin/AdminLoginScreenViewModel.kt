package net.matsudamper.money.frontend.common.viewmodel.admin

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenController
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminLoginScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

public class AdminLoginScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
    private val controller: AdminScreenController,
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
            val result = adminQuery.adminLogin(password)

            val isSuccess = result.data?.adminMutation?.adminLogin?.isSuccess ?: false
            if (isSuccess) {
                controller.navigateToRoot()
            } else {
                result.errors.orEmpty().map {
                    it.message
                }
            }
        }
    }

    private data class ViewModelState(
        val password: TextFieldValue = TextFieldValue(),
        val isLoggedIn: Boolean = false,
    )
}
