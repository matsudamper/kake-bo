package net.matsudamper.money.ui.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import net.matsudamper.money.frontend.common.base.nav.admin.rememberAdminScreenController
import net.matsudamper.money.frontend.common.ui.screen.admin.AdminRootScreen
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminAddUserScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminLoginScreenViewModel
import net.matsudamper.money.frontend.common.viewmodel.admin.AdminRootScreenViewModel

@Composable
internal fun AdminContainer(
    windowInsets: PaddingValues,
) {
    val koin = LocalKoin.current
    val coroutineScope = rememberCoroutineScope()
    val controller = rememberAdminScreenController()

    val adminRootViewModel = remember(coroutineScope, controller) {
        AdminRootScreenViewModel(
            controller = controller,
            coroutineScope = coroutineScope,
            graphqlClient = koin.get(),
        )
    }
    AdminRootScreen(
        adminScreenController = controller,
        adminLoginScreenUiStateProvider = {
            val loginScreenCoroutineScope = rememberCoroutineScope()
            val loginViewModel = remember(loginScreenCoroutineScope, controller) {
                AdminLoginScreenViewModel(
                    coroutineScope = loginScreenCoroutineScope,
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
            val loginScreenCoroutineScope = rememberCoroutineScope()
            val adminAddUserScreenViewModel = remember(loginScreenCoroutineScope, controller) {
                AdminAddUserScreenViewModel(
                    coroutineScope = loginScreenCoroutineScope,
                    controller = controller,
                    graphqlClient = koin.get(),
                )
            }
            adminAddUserScreenViewModel.uiStateFlow.collectAsState().value
        },
        windowInsets = windowInsets,
    )
}
