package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature

internal interface ViewModelProvider<T> {
    @Composable
    public fun get(id: String): T

    @Composable
    public fun get(): T
}

internal expect fun <T : Any> createViewModelProvider(
    factory: (ScopedObjectFeature) -> T,
    kClass: KClass<T>,
): ViewModelProvider<T>

@Deprecated("use LocalScopedObjectStore.current.putOrGet(Unit)")
internal inline fun <reified T : Any> createViewModelProvider(
    noinline factory: (ScopedObjectFeature) -> T,
): ViewModelProvider<T> {
    return createViewModelProvider(
        factory = factory,
        kClass = T::class,
    )
}

@Composable
@Deprecated("use LocalScopedObjectStore.current.putOrGet(Unit)")
internal inline fun <reified T : Any> provideViewModel(
    noinline factory: (ScopedObjectFeature) -> T,
): T {
    return createViewModelProvider(
        factory = factory,
        kClass = T::class,
    ).get()
}
