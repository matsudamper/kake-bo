package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
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
    NavHostScopeProvider(
        navController = navController,
    ) {
        val currentBackstackEntry = navController.currentBackstackEntry
        if (currentBackstackEntry != null) {
            entryProvider(navController.currentBackstackEntry!!).Content()
        }
    }
}
