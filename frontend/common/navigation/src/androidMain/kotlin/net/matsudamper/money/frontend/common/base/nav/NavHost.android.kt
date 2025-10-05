package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public actual fun NavHost(
    navController: ScreenNavController,
    entryProvider: (IScreenStructure) -> NavEntry<IScreenStructure>,
) {
    InternalNavHost(
        navController = navController,
        entryProvider = entryProvider,
    )
}

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
