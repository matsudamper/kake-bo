package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable

interface ViewModelProvider<T> {
    @Composable
    fun get(): T
}

expect fun <T> createViewModelProvider(factory: () -> T): ViewModelProvider<T>
