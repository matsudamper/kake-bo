package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
internal fun NavHostScopeProvider(
    navController: ScreenNavController,
    savedStateHolder: SaveableStateHolder = rememberSaveableStateHolder(),
    scopedObjectStoreOwner: ScopedObjectStoreOwner,
    content: @Composable () -> Unit,
) {
    run {
        var beforeScopeKey: List<String> by rememberSaveable { mutableStateOf(listOf()) }
        LaunchedEffect(navController) {
            snapshotFlow { navController.savedScopeKeys }
                .collect { savedScopeKeys ->
                    val removeScopeKeys = beforeScopeKey.filterNot { it in savedScopeKeys }
                    for (removeScope in removeScopeKeys) {
                        savedStateHolder.removeState(removeScope)
                    }
                    beforeScopeKey = savedScopeKeys.toList()
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
    val currentBackstackEntry = navController.currentBackstackEntry
    if (currentBackstackEntry != null) {
        content()
    }
}
