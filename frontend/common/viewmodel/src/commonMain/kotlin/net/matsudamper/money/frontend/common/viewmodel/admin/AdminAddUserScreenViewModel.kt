package net.matsudamper.money.frontend.common.viewmodel.admin

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
    private val graphqlClient: GraphqlAdminQuery,
    private val controller: AdminScreenController,
) : CommonViewModel(scopedObjectFeature) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AdminAddUserUiState> =
        MutableStateFlow(
            AdminAddUserUiState(
                onChangeUserName = {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(userName = it)
                    }
                },
                onChangePassword = {
                    viewModelStateFlow.update { viewModelState ->
                        viewModelState.copy(password = it)
                    }
                },
                onClickAddButton = {
                    viewModelScope.launch {
                        val result =
                            graphqlClient.addUser(
                                userName = viewModelStateFlow.value.userName,
                                password = viewModelStateFlow.value.password,
                            )
                        println("data: ${result.data}")
                        println("errors: ${result.data?.adminMutation?.addUser?.errorType.orEmpty().joinToString(",")}")
                        println(result.errors?.joinToString { it.message })
                    }
                },
            ),
        ).also { uiStateFlow ->
        }.asStateFlow()

    private data class ViewModelState(
        val userName: String = "",
        val password: String = "",
    )
}
