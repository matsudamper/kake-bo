package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenController
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenControllerImpl
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenType
import net.matsudamper.money.frontend.common.ui.layout.BackHandler

@Composable
public fun AdminRootScreen(
    adminScreenController: AdminScreenController,
    adminRootScreenUiStateProvider: @Composable () -> AdminRootScreenUiState,
    adminLoginScreenUiStateProvider: @Composable () -> AdminLoginScreenUiState,
    adminAddUserUiStateProvider: @Composable () -> AdminAddUserUiState,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val adminScreenControllerImpl = adminScreenController as AdminScreenControllerImpl
    val saveableStateHolder = rememberSaveableStateHolder()
    val screenStack = adminScreenControllerImpl.screen.collectAsState().value
    BackHandler(screenStack.size > 1) {
        adminScreenControllerImpl.popBackStack()
    }
    Box(modifier = modifier.padding(windowInsets)) {
        when (screenStack.lastOrNull()) {
            null,
            AdminScreenType.Login,
            -> {
                val uiState = adminLoginScreenUiStateProvider()
                AdminLoginScreen(
                    uiState = uiState,
                )
            }

            AdminScreenType.Root -> {
                saveableStateHolder.SaveableStateProvider(AdminScreenType.Root.name) {
                    val uiState = adminRootScreenUiStateProvider()
                    AdminRootScreen(
                        uiState = uiState,
                    )
                }
            }

            AdminScreenType.AddUser -> {
                val uiState = adminAddUserUiStateProvider()
                AddUserScreen(
                    uiState = uiState,
                )
            }
        }
    }
}
