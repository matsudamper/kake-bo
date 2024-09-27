package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.reflect.KClass
import net.matsudamper.money.frontend.common.viewmodel.ViewModelFeature

internal actual fun <T : Any> createViewModelProvider(
    factory: (ViewModelFeature) -> T,
    kClass: KClass<T>,
): ViewModelProvider<T> {
    return ViewModelProviderImpl(
        factory = factory,
        kClass = kClass,
    )
}

private class ViewModelProviderImpl<T : Any>(
    private val factory: (ViewModelFeature) -> T,
    private val kClass: KClass<T>,
) : ViewModelProvider<T> {
    @Composable
    override fun get(): T {
        return get(id = "")
    }

    @Composable
    override fun get(id: String): T {
        val coroutineViewModel = viewModel(
            key = ViewModelKey(
                featureClassName = PlatformCoroutineViewModel.viewModelKey,
                viewModelClassName = kClass.simpleName!!,
                id = id,
            ).toString(),
        ) {
            PlatformCoroutineViewModel()
        }

        return viewModel(
            key = ViewModelKey(
                featureClassName = PlatformViewModelWrapper.viewModelKey,
                viewModelClassName = kClass.simpleName!!,
                id = id,
            ).toString(),
        ) {
            PlatformViewModelWrapper(factory(ViewModelFeatureImpl(coroutineViewModel)))
        }.viewModel
    }
}

private class PlatformViewModelWrapper<T>(
    val viewModel: T,
) : ViewModel() {
    companion object {
        val viewModelKey = PlatformViewModelWrapper::class.simpleName!!
    }
}

private class PlatformCoroutineViewModel : ViewModel() {
    companion object {
        val viewModelKey = PlatformCoroutineViewModel::class.simpleName!!
    }
}

private class ViewModelFeatureImpl(
    private val coroutineViewModel: PlatformCoroutineViewModel,
) : ViewModelFeature {
    override val coroutineScope get() = coroutineViewModel.viewModelScope
}

private data class ViewModelKey(
    val featureClassName: String,
    val viewModelClassName: String,
    val id: String,
)
