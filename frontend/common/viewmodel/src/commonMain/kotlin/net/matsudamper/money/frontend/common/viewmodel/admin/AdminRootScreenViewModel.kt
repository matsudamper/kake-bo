package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenController
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreenUiState
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

public class AdminRootScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val graphqlClient: GraphqlAdminQuery,
    private val controller: AdminScreenController,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())

    public val uiStateFlow: StateFlow<AdminRootScreenUiState> = MutableStateFlow(
        AdminRootScreenUiState(
            listener = object : AdminRootScreenUiState.Listener {
                override fun onClickAddUser() {
                    controller.navigateToAddUser()
                }
            },
        ),
    ).also { uiStateFlow ->
    }.asStateFlow()

    private data class ViewModelState(
        val any: Any? = null,
    )
}
