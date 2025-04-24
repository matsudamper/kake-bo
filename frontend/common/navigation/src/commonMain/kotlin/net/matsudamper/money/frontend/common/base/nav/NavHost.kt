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
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public fun NavHost(
    navController: ScreenNavController,
    content: @Composable (ScreenNavController.NavStackEntry) -> Unit,
) {
    val scopedObjectStoreOwner = rememberScopedObjectStoreOwner("NavHost")
    val holder = rememberSaveableStateHolder("NavHostSaveableStateHolder")
    run {
        var beforeEntries: List<ScreenNavController.NavStackEntry> by remember { mutableStateOf(listOf()) }
        LaunchedEffect(navController) {
            snapshotFlow { navController.backstackEntries }
                .collect {
                    val removedEntries = beforeEntries.filterNot { it in navController.backstackEntries }
                        .filterNot { it.isHome }
                        .filterNot { it.savedState }
                    for (entry in removedEntries) {
                        holder.removeState(entry.structure.sameScreenId)
                    }

                    beforeEntries = navController.backstackEntries
                }
        }
    }
    LaunchedEffect(navController.backstackEntries) {
        for (entry in navController.backstackEntries) {
            scopedObjectStoreOwner.createOrGetScopedObjectStore(entry.structure.sameScreenId)
        }
        val aliveSameScreenIdList = navController.backstackEntries.map { it.structure.sameScreenId }
        scopedObjectStoreOwner.keys()
            .mapNotNull { it as? String }
            .filterNot { it in aliveSameScreenIdList }
            .forEach { sameScreenId ->
                scopedObjectStoreOwner.removeScopedObjectStore(sameScreenId)
            }
    }
    CompositionLocalProvider(
        LocalScopedObjectStore provides scopedObjectStoreOwner
            .createOrGetScopedObjectStore(navController.currentBackstackEntry.structure.sameScreenId),
    ) {
        holder.SaveableStateProvider(navController.currentBackstackEntry.structure.sameScreenId) {
            content(navController.currentBackstackEntry)
        }
    }
}

public interface ScopedObjectStoreOwner {
    public fun createOrGetScopedObjectStore(key: Any): ScopedObjectStore
    public fun removeScopedObjectStore(key: Any)
    public fun keys(): Set<Any>
}

@Composable
public expect fun rememberScopedObjectStoreOwner(key: String): ScopedObjectStoreOwner
