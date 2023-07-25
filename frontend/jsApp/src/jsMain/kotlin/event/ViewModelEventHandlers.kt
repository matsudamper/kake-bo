package event

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.ScreenNavController
import net.matsudamper.money.frontend.common.viewmodel.lib.EventHandler
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent
import net.matsudamper.money.frontend.common.viewmodel.root.MailImportViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.MailLinkViewModel
import net.matsudamper.money.frontend.common.viewmodel.root.home.HomeViewModel

data class ViewModelEventHandlers(
    private val navController: ScreenNavController,
    private val globalEventSender: EventSender<GlobalEvent>,
) {
    suspend fun handle(handler: EventHandler<HomeViewModel.Event>) {
        coroutineScope {
            handler.collect(
                object : HomeViewModel.Event {
                    override fun navigateToMailImport() {
                        navController.navigate(Screen.MailImport)
                    }

                    override fun navigateToMailLink() {
                        navController.navigate(Screen.MailLink)
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<MailImportViewModel.Event>) {
        coroutineScope {
            val scope = this
            handler.collect(
                object : MailImportViewModel.Event {
                    override fun backRequest() {
                        navController.back()
                    }

                    override fun globalToast(message: String) {
                        scope.launch {
                            globalEventSender.send {
                                it.showSnackBar(message)
                            }
                        }
                    }
                },
            )
        }
    }

    suspend fun handle(handler: EventHandler<MailLinkViewModel.Event>) {
        coroutineScope {
            val scope = this
            handler.collect(
                object : MailLinkViewModel.Event {
                    override fun backRequest() {
                        navController.back()
                    }

                    override fun globalToast(message: String) {
                        scope.launch {
                            globalEventSender.send {
                                it.showSnackBar(message)
                            }
                        }
                    }
                },
            )
        }
    }
}