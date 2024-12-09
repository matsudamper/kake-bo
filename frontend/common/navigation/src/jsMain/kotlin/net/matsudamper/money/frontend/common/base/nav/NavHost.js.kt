package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberScopedObjectStoreOwner(): ScopedObjectStoreOwner {
    return remember { NavViewModel() }
}

private class NavViewModel() : ScopedObjectStoreOwner {
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
