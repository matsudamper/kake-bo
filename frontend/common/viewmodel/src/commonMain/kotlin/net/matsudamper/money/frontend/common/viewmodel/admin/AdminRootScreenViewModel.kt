package net.matsudamper.money.frontend.common.viewmodel.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenController
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreenUiState
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

public class AdminRootScreenViewModel(
    scopedObjectFeature: ScopedObjectFeature,
    private val adminQuery: GraphqlAdminQuery,
    private val controller: AdminScreenController,
) : CommonViewModel(scopedObjectFeature) {
    init {
        viewModelScope.launch {
            when (adminQuery.isLoggedIn()) {
                GraphqlAdminQuery.IsLoggedInResult.LoggedIn -> {
                    controller.navigateToRoot()
                }

                GraphqlAdminQuery.IsLoggedInResult.NotLoggedIn,
                GraphqlAdminQuery.IsLoggedInResult.ServerError,
                -> {
                    controller.navigateToLogin()
                }
            }
        }
    }

    public val uiStateFlow: StateFlow<AdminRootScreenUiState> = MutableStateFlow(
        AdminRootScreenUiState(
            listener = object : AdminRootScreenUiState.Listener {
                override fun onClickAddUser() {
                    controller.navigateToAddUser()
                }
            },
        ),
    ).asStateFlow()
}
