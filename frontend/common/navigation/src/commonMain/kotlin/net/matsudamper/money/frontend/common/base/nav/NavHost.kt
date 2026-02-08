package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    onBack: () -> Unit = {
        if (navController.canGoBack) {
            navController.back()
        }
    },
) {
    val backStack by rememberUpdatedState(navController.backstackEntries)

    LaunchedEffect(navController.currentBackstackEntry) {
        Logger.d("Navigation", "${navController.currentBackstackEntry}")
    }
    val holder = rememberSaveableStateHolder("nav_display")
    val scopedObjectStoreOwner = rememberScopedObjectStoreOwner("NavHost")
    NavHostScopeLifecycleHandler(
        navController = navController,
        savedStateHolder = holder,
        scopedObjectStoreOwner = scopedObjectStoreOwner,
    )
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider,
        entryDecorators =
        remember {
            listOf(
                NavEntryDecorator(
                    onPop = {
                        // NavHostScopeProviderで削除を管理する
                    },
                    decorate = { entry ->
                        val structure = entry.contentKey as IScreenStructure

                        holder.SaveableStateProvider(structure.scopeKey) {
                            entry.Content()
                        }
                    },
                ),
                NavEntryDecorator(
                    onPop = {
                        // NavHostScopeProviderで削除を管理する
                    },
                    decorate = { entry ->
                        val structure = entry.contentKey as IScreenStructure

                        CompositionLocalProvider(
                            LocalScopedObjectStore provides scopedObjectStoreOwner
                                .createOrGetScopedObjectStore(structure.sameScreenId),
                        ) {
                            entry.Content()
                        }
                    },
                ),
            )
        },
        onBack = onBack,
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
