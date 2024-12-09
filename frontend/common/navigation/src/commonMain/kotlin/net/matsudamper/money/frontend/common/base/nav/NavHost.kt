package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public fun NavHost(
    navController: ScreenNavController,
    content: @Composable (ScreenNavController.NavStackEntry) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    val scopedObjectStoreOwner = rememberScopedObjectStoreOwner()
    DisposableEffect(Unit) {
        val listener = object : ScreenNavController.RemovedBackstackEntryListener {
            override fun onRemoved(entry: ScreenNavController.NavStackEntry) {
                holder.removeState(entry.structure.toString())
            }
        }
        navController.addRemovedBackstackEntryListener(listener)
        onDispose {
            navController.removeRemovedBackstackEntryListener(listener)
        }
    }
    LaunchedEffect(navController.backstackEntries) {
        for (entry in navController.backstackEntries) {
            scopedObjectStoreOwner.createOrGetScopedObjectStore(entry.structure)
        }
        val backStackStructures = navController.backstackEntries.map { it.structure }
        scopedObjectStoreOwner.keys()
            .filterNot { it in backStackStructures }
            .forEach {
                scopedObjectStoreOwner.removeScopedObjectStore(it)
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

internal interface ScopedObjectStoreOwner {
    fun createOrGetScopedObjectStore(key: Any): ScopedObjectStore
    fun removeScopedObjectStore(key: Any)
    fun keys(): Set<Any>
}

@Composable
internal expect fun rememberScopedObjectStoreOwner(): ScopedObjectStoreOwner
