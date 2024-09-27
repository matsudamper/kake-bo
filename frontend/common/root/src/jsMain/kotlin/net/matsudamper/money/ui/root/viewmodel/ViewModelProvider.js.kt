package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.reflect.KClass

actual fun <T : Any> createViewModelProvider(
    factory: () -> T,
    kClass: KClass<T>,
): ViewModelProvider<T> {
    return ViewModelProviderImpl(
        factory = factory,
    )
}

private class ViewModelProviderImpl<T : Any>(
    private val factory: () -> T,
) : ViewModelProvider<T> {
    @Composable
    override fun get(): T {
        return get(id = "")
    }

    @Composable
    override fun get(id: String): T {
        return remember { factory() }
    }
}
