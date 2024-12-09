package net.matsudamper.money.frontend.common.base.nav

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

public class ScopedObjectStore {
    private val store = mutableMapOf<Any, StoreObject>()
    public fun clear(id: Any) {
        store.remove(id)?.scopedObjectFeature?.coroutineScope?.cancel()
    }

    public fun <T : Any> putOrGet(id: Any, provider: (ScopedObjectFeature) -> T): T {
        val item = store[id]
        if (item != null) {
            @Suppress("UNCHECKED_CAST")
            return item.item as T
        } else {
            val feature = ScopedObjectFeatureImpl(CoroutineScope(Job()))
            val created = provider(feature)
            return created.also {
                store[id] = StoreObject(it, feature)
            }
        }
    }

    public fun clearAll() {
        store.forEach { it.value.scopedObjectFeature.coroutineScope.cancel() }
        store.clear()
    }

    private class StoreObject(
        val item: Any,
        val scopedObjectFeature: ScopedObjectFeature,
    )
}

private class ScopedObjectFeatureImpl(
    override val coroutineScope: CoroutineScope,
) : ScopedObjectFeature
