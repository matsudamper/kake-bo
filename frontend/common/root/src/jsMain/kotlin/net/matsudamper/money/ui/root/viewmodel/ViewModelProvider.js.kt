package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable

actual fun <T> createViewModelProvider(factory: () -> T): ViewModelProvider<T> {
    return object : ViewModelProvider<T> {
        @Composable
        override fun get(): T {
            return factory()
        }
    }
}
