package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.matsudamper.money.frontend.common.viewmodel.ViewModelFeature

internal actual fun <T : Any> createViewModelProvider(
    factory: (ViewModelFeature) -> T,
    kClass: KClass<T>,
): ViewModelProvider<T> {
    return ViewModelProviderImpl(
        factory = factory,
    )
}

private class ViewModelProviderImpl<T : Any>(
    private val factory: (ViewModelFeature) -> T,
) : ViewModelProvider<T> {
    @Composable
    override fun get(): T {
        return get(id = "")
    }

    @Composable
    override fun get(id: String): T {
        return remember {
            factory(
                ViewModelProviderScopeImpl(
                    // TODO Navigationと共にLifecycleを作成する
                    coroutineScope = CoroutineScope(Job()),
                ),
            )
        }
    }
}

private class ViewModelProviderScopeImpl(
    override val coroutineScope: CoroutineScope,
) : ViewModelFeature
