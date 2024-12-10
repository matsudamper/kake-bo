package net.matsudamper.money.frontend.common.base.nav

import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

public class ScopedObjectStore {
    private val store = mutableMapOf<Key, StoreObject>()
    public fun clear(id: Any, clazz: KClass<*>) {
        store.remove(Key(id, clazz))?.scopedObjectFeature?.coroutineScope?.cancel()
    }

    public inline fun <reified T : Any> putOrGet(id: Any, noinline provider: (ScopedObjectFeature) -> T): T {
        return putOrGet(id, T::class, provider)
    }

    public fun <T : Any> putOrGet(id: Any, clazz: KClass<T>,provider: (ScopedObjectFeature) -> T): T {
        val item = store[Key(id, clazz)]
        if (item != null) {
            @Suppress("UNCHECKED_CAST")
            return item.item as T
        } else {
            val feature = ScopedObjectFeatureImpl(CoroutineScope(Job()))
            val created = provider(feature)
            return created.also {
                store[Key(id, clazz)] = StoreObject(it, feature)
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

    private data class Key(val id: Any, val clazz: KClass<*>)
}

private class ScopedObjectFeatureImpl(
    override val coroutineScope: CoroutineScope,
) : ScopedObjectFeature
