package net.matsudamper.money.frontend.common.viewmodel.admin

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenController
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminAddUserUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

public class AdminAddUserScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
    private val controller: AdminScreenController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AdminAddUserUiState> = MutableStateFlow(
        AdminAddUserUiState(
            userName = TextFieldValue(),
            password = TextFieldValue(),
            onChangeUserName = {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(userName = TextFieldValue(it))
                }
            },
            onChangePassword = {
                viewModelStateFlow.update { viewModelState ->
                    viewModelState.copy(password = TextFieldValue(it))
                }
            },
            onClickAddButton = {
                viewModelScope.launch {
                    val result = adminQuery.addUser(
                        userName = viewModelStateFlow.value.userName.text,
                        password = viewModelStateFlow.value.password.text,
                    )
                    println("data: ${result.data}")
                    println("errors: ${result.data?.adminMutation?.addUser?.errorType.orEmpty().joinToString(",")}")
                    println(result.errors?.joinToString { it.message })
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

    private data class ViewModelState(
        val userName: TextFieldValue = TextFieldValue(),
        val password: TextFieldValue = TextFieldValue(),
    )
}
