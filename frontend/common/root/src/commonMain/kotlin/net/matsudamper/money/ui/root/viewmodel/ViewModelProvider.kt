package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

internal interface ViewModelProvider<T> {
    @Composable
    public fun get(id: String): T

    @Composable
    public fun get(): T
}

internal expect fun <T : Any> createViewModelProvider(
    factory: () -> T,
    kClass: KClass<T>,
): ViewModelProvider<T>

internal inline fun <reified T : Any> createViewModelProvider(
    noinline factory: () -> T,
): ViewModelProvider<T> {
    return createViewModelProvider(
        factory = factory,
        kClass = T::class,
    )
}
