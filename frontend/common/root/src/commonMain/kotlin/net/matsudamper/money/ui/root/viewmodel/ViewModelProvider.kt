package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass
import net.matsudamper.money.frontend.common.viewmodel.ViewModelFeature

internal interface ViewModelProvider<T> {
    @Composable
    public fun get(id: String): T

    @Composable
    public fun get(): T
}

internal expect fun <T : Any> createViewModelProvider(
    factory: (ViewModelFeature) -> T,
    kClass: KClass<T>,
): ViewModelProvider<T>

internal inline fun <reified T : Any> createViewModelProvider(
    noinline factory: (ViewModelFeature) -> T,
): ViewModelProvider<T> {
    return createViewModelProvider(
        factory = factory,
        kClass = T::class,
    )
}

@Composable
internal inline fun <reified T : Any> provideViewModel(
    noinline factory: (ViewModelFeature) -> T,
): T {
    return createViewModelProvider(
        factory = factory,
        kClass = T::class,
    ).get()
}
