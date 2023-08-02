package net.matsudamper.money.frontend.common.viewmodel.admin

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenController
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminLoginScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

public class AdminLoginScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlClient: GraphqlAdminQuery,
    private val controller: AdminScreenController,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AdminLoginScreenUiState> = MutableStateFlow(
        AdminLoginScreenUiState(
            onChangePassword = {
                viewModelStateFlow.update { state -> state.copy(password = it) }
            },
            onClickLogin = {
                login(viewModelStateFlow.value.password.text)
            },
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect {
            }
        }
    }.asStateFlow()

    private fun login(password: String) {
        // TODO: validate  "!@#$%^&*()_+-?<>,."
        coroutineScope.launch {
            val result = graphqlClient.adminLogin(password)

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
