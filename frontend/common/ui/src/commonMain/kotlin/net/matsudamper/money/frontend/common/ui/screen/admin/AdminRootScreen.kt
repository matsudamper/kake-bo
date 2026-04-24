package net.matsudamper.money.frontend.common.ui.screen.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenController
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenControllerImpl
import net.matsudamper.money.frontend.common.base.nav.admin.AdminScreenType

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
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(windowInsets),
    ) {
        if (screenStack.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            NavDisplay(
                backStack = screenStack,
                modifier = Modifier.fillMaxSize(),
                sceneStrategy = DialogSceneStrategy(),
                onBack = {
                    adminScreenController.popBackStack()
                },
                entryProvider = { screen ->
                    when (screen) {
                        AdminScreenType.Login -> {
                            NavEntry(
                                key = screen,
                            ) {
                                val uiState = adminLoginScreenUiStateProvider()
                                AdminLoginScreen(
                                    uiState = uiState,
                                )
                            }
                        }

                        AdminScreenType.Root -> {
                            NavEntry(
                                key = screen,
                            ) {
                                saveableStateHolder.SaveableStateProvider(AdminScreenType.Root.name) {
                                    val uiState = adminRootScreenUiStateProvider()
                                    AdminRootScreen(
                                        uiState = uiState,
                                    )
                                }
                            }
                        }

                        AdminScreenType.AddUser -> {
                            NavEntry(
                                key = screen,
                                metadata = DialogSceneStrategy.dialog(
                                    dialogProperties = DialogProperties(
                                        usePlatformDefaultWidth = false,
                                    ),
                                ),
                            ) {
                                val uiState = adminAddUserUiStateProvider()
                                AddUserScreen(
                                    uiState = uiState,
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}
