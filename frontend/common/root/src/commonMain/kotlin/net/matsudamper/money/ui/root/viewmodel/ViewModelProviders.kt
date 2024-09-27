package net.matsudamper.money.ui.root.viewmodel

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavControllerImpl
import net.matsudamper.money.frontend.common.viewmodel.root.RootViewModel
import org.koin.core.Koin

internal class ViewModelProviders(
    private val koin: Koin,
    private val navController: ScreenNavControllerImpl,
    private val rootCoroutineScope: CoroutineScope,
) {
    @Composable
    public fun rootViewModel(): RootViewModel {
        return createViewModelProvider {
            RootViewModel(
                loginCheckUseCase = koin.get(),
                coroutineScope = rootCoroutineScope,
                navController = navController,
            )
        }.get()
    }
}
