package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public fun NavHost(
    navController: ScreenNavController,
    content: @Composable (ScreenNavController.NavStackEntry) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    val scopedObjectStoreOwner = rememberScopedObjectStoreOwner("NavHost")
    run {
        var beforeEntries: List<ScreenNavController.NavStackEntry> by remember { mutableStateOf(listOf()) }
        LaunchedEffect(navController.backstackEntries) {
            beforeEntries
                .filterNot { navController.backstackEntries.contains(it) }
                .forEach { entry ->
                    holder.removeState(entry.structure.toString())
                }
            beforeEntries = navController.backstackEntries
        }
    }
    LaunchedEffect(navController.backstackEntries) {
        for (entry in navController.backstackEntries) {
            scopedObjectStoreOwner.createOrGetScopedObjectStore(entry.structure)
        }
        val backStackStructures = navController.backstackEntries.map { it.structure }
        scopedObjectStoreOwner.keys()
            .map { it as IScreenStructure }
            .filterNot { it in backStackStructures }
            .forEach { structure ->
                scopedObjectStoreOwner.removeScopedObjectStore(structure)
            }
    }
    CompositionLocalProvider(
        LocalScopedObjectStore provides scopedObjectStoreOwner.createOrGetScopedObjectStore(navController.currentBackstackEntry.structure),
    ) {
        val entry = navController.currentBackstackEntry
        holder.SaveableStateProvider(entry.structure.toString()) {
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
