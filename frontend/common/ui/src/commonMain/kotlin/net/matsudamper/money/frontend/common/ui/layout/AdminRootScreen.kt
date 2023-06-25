package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.base.AdminScreenController
import net.matsudamper.money.frontend.common.base.AdminScreenControllerImpl
import net.matsudamper.money.frontend.common.base.AdminScreenType
import net.matsudamper.money.frontend.common.uistate.AdminAddUserUiState
import net.matsudamper.money.frontend.common.uistate.AdminLoginScreenUiState
import net.matsudamper.money.frontend.common.uistate.AdminRootScreenUiState

@Composable
public fun AdminRootScreen(
    adminScreenController: AdminScreenController,
    adminRootScreenUiStateProvider: @Composable () -> AdminRootScreenUiState,
    adminLoginScreenUiStateProvider: @Composable () -> AdminLoginScreenUiState,
    adminAddUserUiStateProvider: @Composable () -> AdminAddUserUiState,
) {
    val adminScreenControllerImpl = adminScreenController as AdminScreenControllerImpl
    val saveableStateHolder = rememberSaveableStateHolder()
    val screenStack = adminScreenControllerImpl.screen.collectAsState().value
    BackHandler(screenStack.size > 1) {
        adminScreenControllerImpl.popBackStack()
    }
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
