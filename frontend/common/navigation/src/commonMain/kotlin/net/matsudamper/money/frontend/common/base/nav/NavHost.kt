package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import net.matsudamper.money.frontend.common.base.lib.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public expect fun NavHost(
    navController: ScreenNavController,
    content: @Composable (IScreenStructure) -> Unit,
)

@Composable
public fun InternalNavHost(
    navController: ScreenNavController,
    content: @Composable (IScreenStructure) -> Unit,
) {
    val scopedObjectStoreOwner = rememberScopedObjectStoreOwner("NavHost")
    val holder = rememberSaveableStateHolder("NavHostSaveableStateHolder")
    run {
        var beforeScopeKey: List<String> by remember { mutableStateOf(listOf()) }
        LaunchedEffect(navController) {
            snapshotFlow { navController.savedScopeKeys }
                .collect { savedScopeKeys ->
                    val removeScopeKeys = beforeScopeKey.filterNot { it in savedScopeKeys }
                    for (removeScope in removeScopeKeys) {
                        holder.removeState(removeScope)
                    }
                }
        }
    }
    LaunchedEffect(navController.savedScopeKeys) {
        for (scopeKey in navController.savedScopeKeys) {
            scopedObjectStoreOwner.createOrGetScopedObjectStore(scopeKey)
        }
        val aliveScopeKey = navController.savedScopeKeys
        scopedObjectStoreOwner.keys()
            .mapNotNull { it as? String }
            .filterNot { it in aliveScopeKey }
            .forEach { scopeKey ->
                scopedObjectStoreOwner.removeScopedObjectStore(scopeKey)
            }
    }
    CompositionLocalProvider(
        LocalScopedObjectStore provides scopedObjectStoreOwner
            .createOrGetScopedObjectStore(navController.currentBackstackEntry.sameScreenId),
    ) {
        holder.SaveableStateProvider(navController.currentBackstackEntry.sameScreenId) {
            content(navController.currentBackstackEntry)
        }
    }
}


public interface ScopedObjectStoreOwner {
    public fun createOrGetScopedObjectStore(key: Any): ScopedObjectStore
    public fun removeScopedObjectStore(key: Any)
    public fun keys(): Set<Any>
}

internal class InMemoryScopedObjectStoreOwnerImpl() : ScopedObjectStoreOwner {
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
public expect fun rememberScopedObjectStoreOwner(key: String): ScopedObjectStoreOwner
