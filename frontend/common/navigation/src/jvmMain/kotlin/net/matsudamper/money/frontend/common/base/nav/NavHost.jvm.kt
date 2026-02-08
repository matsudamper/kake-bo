package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
public actual fun rememberScopedObjectStoreOwner(key: String): ScopedObjectStoreOwner {
    val owner = viewModel(
        key = key,
        initializer = {
            InMemoryScopedObjectStoreOwnerImplViewModel()
        },
    )
    return owner
}

private class InMemoryScopedObjectStoreOwnerImplViewModel :
    ViewModel(),
    ScopedObjectStoreOwner by InMemoryScopedObjectStoreOwnerImpl()
