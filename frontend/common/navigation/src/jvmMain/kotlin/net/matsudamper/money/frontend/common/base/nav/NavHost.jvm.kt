package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.lib.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public actual fun rememberScopedObjectStoreOwner(key: String): ScopedObjectStoreOwner {
    val owner = viewModel(
        key = key,
        initializer = {
            InMemoryScopedObjectStoreOwnerImplViewModel()
        },
    )
    return owner
}

private class InMemoryScopedObjectStoreOwnerImplViewModel :
    ViewModel(),
    ScopedObjectStoreOwner by InMemoryScopedObjectStoreOwnerImpl()

@Composable
public actual fun NavHost(
    navController: ScreenNavController,
    entryProvider: (IScreenStructure) -> NavEntry<IScreenStructure>,
) {
    val backStack by rememberUpdatedState(navController.backstackEntries)

    LaunchedEffect(navController.currentBackstackEntry) {
        Logger.d("Navigation", "${navController.currentBackstackEntry}")
    }
    val holder = rememberSaveableStateHolder("nav_display")
    NavHostScopeProvider(
        navController = navController,
        savedStateHolder = holder,
    ) {
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider,
            entryDecorators = listOf(
                remember {
                    NavEntryDecorator(
                        onPop = {
                        },
                        decorate = { entry ->
                            val structure = entry.contentKey as IScreenStructure

                            holder.SaveableStateProvider(structure.scopeKey) {
                                entry.Content()
                            }
                        },
                    )
                },
            ),
            onBack = {
                if (navController.canGoBack) {
                    navController.back()
                }
            },
        )
    }
}
