package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import net.matsudamper.money.frontend.common.base.Logger
import net.matsudamper.money.frontend.common.base.lib.rememberSaveableStateHolder
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public fun NavHost(
    navController: ScreenNavController,
    entryProvider: (IScreenStructure) -> NavEntry<IScreenStructure>,
) {
    val backStack by rememberUpdatedState(navController.backstackEntries)

    LaunchedEffect(navController.currentBackstackEntry) {
        Logger.d("Navigation", "${navController.currentBackstackEntry}")
    }
    val holder = rememberSaveableStateHolder("nav_display")
    NavHostScopeProvider(
        navController = navController,
        savedStateHolder = holder,
    ) {
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider,
            entryDecorators = listOf(
                remember {
                    NavEntryDecorator(
                        onPop = {
                        },
                        decorate = { entry ->
                            val structure = entry.contentKey as IScreenStructure

                            holder.SaveableStateProvider(structure.scopeKey) {
                                entry.Content()
                            }
                        },
                    )
                },
            ),
            onBack = {
                if (navController.canGoBack) {
                    navController.back()
                }
            },
            predictivePopTransitionSpec = {
                ContentTransform(
                    targetContentEnter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = 1600.0f,
                        ),
                    ),
                    initialContentExit = fadeOut(
                        animationSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = 1600.0f,
                        ),
                    ),
                )
            },
        )
    }
}

@Composable
public fun NavHostScopeProvider(
    navController: ScreenNavController,
    savedStateHolder: SaveableStateHolder = rememberSaveableStateHolder(),
    content: @Composable () -> Unit,
) {
    val scopedObjectStoreOwner = rememberScopedObjectStoreOwner("NavHost")
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
        CompositionLocalProvider(
            LocalScopedObjectStore provides scopedObjectStoreOwner
                .createOrGetScopedObjectStore(currentBackstackEntry.sameScreenId),
        ) {
            content()
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
