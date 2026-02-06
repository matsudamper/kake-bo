package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavEntry
import net.matsudamper.money.frontend.common.base.lifecycle.LocalScopedObjectStore
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public expect fun NavHost(
    navController: ScreenNavController,
    entryProvider: (IScreenStructure) -> NavEntry<IScreenStructure>,
)

/**
 * @param execSavableStateProvider JSとJVMでNav3を使うまでの間の暫定対応
 */
@Composable
public fun NavHostScopeProvider(
    navController: ScreenNavController,
    savedStateHolder: SaveableStateHolder = rememberSaveableStateHolder(),
    execSavableStateProvider: Boolean = false,
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
            // createOrGetScopedObjectStoreでScopedObjectStoreが再生成される事によって別のインスタンスが生成される事によって不具合が生じている
            // ホームから別の画面に切り替えた瞬間に別のインスタンスが生成されてします。それによってViewModelが再生成されてしまう。
            // なのでViewModelの生成に必要な値をNavDisplay側で入れてあげたほうが良い。完了が完全に分離される
            LocalScopedObjectStore provides scopedObjectStoreOwner
                .createOrGetScopedObjectStore(currentBackstackEntry.sameScreenId),
        ) {
            if (execSavableStateProvider) {
                savedStateHolder.SaveableStateProvider(currentBackstackEntry.scopeKey) {
                    content()
                }
            } else {
                content()
            }
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
