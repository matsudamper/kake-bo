package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

interface ViewModelProvider<T> {
    @Composable
    fun get(id: String): T

    @Composable
    fun get(): T
}

expect fun <T : Any> createViewModelProvider(
    factory: () -> T,
    kClass: KClass<T>,
): ViewModelProvider<T>

inline fun <reified T : Any> createViewModelProvider(
    noinline factory: () -> T,
): ViewModelProvider<T> {
    return createViewModelProvider(
        factory = factory,
        kClass = T::class,
    )
}
