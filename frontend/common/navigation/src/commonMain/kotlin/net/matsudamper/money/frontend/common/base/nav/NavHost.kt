package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

public val LocalScopedObjectStore: ProvidableCompositionLocal<ScopedObjectStore> = compositionLocalOf<ScopedObjectStore> {
    error("No ScopedObjectStore provided")
}

@Composable
public fun NavHost(
    navController: ScreenNavController,
    content: @Composable (ScreenNavController.NavStackEntry) -> Unit,
) {
    val holder = rememberSaveableStateHolder()
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
    CompositionLocalProvider(
        LocalScopedObjectStore provides navController.currentBackstackEntry.scopedObjectStore,
    ) {
        val entry = navController.currentBackstackEntry
        holder.SaveableStateProvider(entry.structure.toString()) {
            content(navController.currentBackstackEntry)
        }
    }
}
