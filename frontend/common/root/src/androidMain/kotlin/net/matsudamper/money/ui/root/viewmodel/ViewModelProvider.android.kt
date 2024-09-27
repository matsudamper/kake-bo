package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.reflect.KClass

actual fun <T : Any> createViewModelProvider(factory: () -> T, kClass: KClass<T>): ViewModelProvider<T> {
    return ViewModelProviderImpl(
        factory = factory,
        kClass = kClass,
    )
}

private class ViewModelProviderImpl<T: Any>(
    private val factory: () -> T,
    private val kClass: KClass<T>,
) : ViewModelProvider<T> {
    @Composable
    override fun get(): T {
        return get(id = "")
    }

    @Composable
    override fun get(id: String): T {
        return viewModel(
            key = ViewModelKey(
                className = kClass.simpleName!!,
                id = id,
            ).toString(),
        ) {
            PlatformViewModelWrapper(factory())
        }.viewModel
    }
}

private class PlatformViewModelWrapper<T>(
    val viewModel: T,
) : ViewModel()

data class ViewModelKey(
    val className: String,
    val id: String,
)
