package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

actual fun <T> createViewModelProvider(factory: () -> T): ViewModelProvider<T> {
    return object : ViewModelProvider<T> {
        @Composable
        override fun get(): T {
            return viewModel {
                PlatformViewModelWrapper(factory())
            }.viewModel
        }
    }
}

private class PlatformViewModelWrapper<T>(
    val viewModel: T,
) : ViewModel()
