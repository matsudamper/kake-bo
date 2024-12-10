package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public fun NavHost(
    navController: ScreenNavController,
    content: @Composable (ScreenNavController.NavStackEntry) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    val scopedObjectStoreOwner = rememberScopedObjectStoreOwner()
    run {
        var beforeEntries: List<ScreenNavController.NavStackEntry> by remember { mutableStateOf(listOf()) }
        LaunchedEffect(navController.backstackEntries) {
            beforeEntries
                .filterNot { navController.backstackEntries.contains(it) }
                .forEach { entry ->
                    scopedObjectStoreOwner.removeScopedObjectStore(entry.structure)
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

internal interface ScopedObjectStoreOwner {
    fun createOrGetScopedObjectStore(key: Any): ScopedObjectStore
    fun removeScopedObjectStore(key: Any)
    fun keys(): Set<Any>
}

@Composable
internal expect fun rememberScopedObjectStoreOwner(): ScopedObjectStoreOwner
