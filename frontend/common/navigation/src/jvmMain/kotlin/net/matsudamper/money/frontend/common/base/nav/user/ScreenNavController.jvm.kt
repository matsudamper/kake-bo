package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectStore

@Composable
public actual fun rememberMainScreenNavController(): ScreenNavController {
    val holder = rememberSaveableStateHolder()
    val navViewModel = viewModel(
        initializer = {
            NavViewModel()
        },
    )
    return remember(holder, navViewModel) {
        ScreenNavControllerImpl(
            initial = RootHomeScreenStructure.Home,
            savedStateHolder = holder,
            navViewModel = navViewModel,
        )
    }
}

internal class NavViewModel() : ViewModel() {
    private val scopedObjectStore = mutableMapOf<Any, ScopedObjectStore>()

    fun createOrGetScopedObjectStore(key: Any): ScopedObjectStore {
        val store = scopedObjectStore[key] ?: ScopedObjectStore()
        scopedObjectStore[key] = store
        return store
    }

    fun removeScopedObjectStore(key: Any) {
        scopedObjectStore.remove(key)?.clearAll()
    }
}
