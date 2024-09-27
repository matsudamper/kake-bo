package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import net.matsudamper.money.frontend.common.base.nav.admin.rememberAdminScreenController
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreen
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminAddUserScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminLoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminRootScreenViewModel
import net.matsudamper.money.ui.root.viewmodel.provideViewModel

@Composable
internal fun AdminContainer(
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val controller = rememberAdminScreenController()

    val adminRootViewModel = provideViewModel {
        AdminRootScreenViewModel(
            viewModelFeature = it,
            controller = controller,
            graphqlClient = koin.get(),
        )
    }
    AdminRootScreen(
        adminScreenController = controller,
        adminLoginScreenUiStateProvider = {
            val loginViewModel = provideViewModel {
                AdminLoginScreenViewModel(
                    viewModelFeature = it,
                    controller = controller,
                    graphqlClient = koin.get(),
                )
            }
            loginViewModel.uiStateFlow.collectAsState().value
        },
        adminRootScreenUiStateProvider = {
            adminRootViewModel.uiStateFlow.collectAsState().value
        },
        adminAddUserUiStateProvider = {
            val adminAddUserScreenViewModel = provideViewModel {
                AdminAddUserScreenViewModel(
                    viewModelFeature = it,
                    controller = controller,
                    graphqlClient = koin.get(),
                )
            }
            adminAddUserScreenViewModel.uiStateFlow.collectAsState().value
        },
        windowInsets = windowInsets,
    )
}
