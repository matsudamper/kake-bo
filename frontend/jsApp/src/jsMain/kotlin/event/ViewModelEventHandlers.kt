package event

import kotlinx.coroutines.coroutineScope
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.ScreenNavController
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.root.home.HomeViewModel

data class ViewModelEventHandlers(
    private val navController: ScreenNavController,
) {
    suspend fun handle(handler: EventHandler<HomeViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : HomeViewModel.Event {
                    override fun navigateToMailImport() {
                        navController.navigate(Screen.MailImport)
                    }
                }
            )
        }
    }
}