package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public actual fun rememberScopedObjectStoreOwner(key: String): ScopedObjectStoreOwner {
    return remember(key) { NavViewModel() }
}

internal class NavViewModel() : ScopedObjectStoreOwner {
    private val scopedObjectStore = mutableMapOf<Any, ScopedObjectStore>()

    override fun createOrGetScopedObjectStore(key: Any): ScopedObjectStore {
        val store = scopedObjectStore[key] ?: ScopedObjectStore()
        scopedObjectStore[key] = store
        return store
    }

    override fun removeScopedObjectStore(key: Any) {
        scopedObjectStore.remove(key)?.clearAll()
    }

    override fun keys(): Set<Any> {
        return scopedObjectStore.keys
    }
}

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
