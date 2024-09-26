package net.matsudamper.money.frontend.common.viewmodel.root.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.usecase.LoginCheckUseCaseImpl
import net.matsudamper.money.frontend.common.viewmodel.lib.EventSender
import net.matsudamper.money.frontend.common.viewmodel.root.GlobalEvent

public class LoginCheckUseCaseEventListenerImpl(
    private val navController: ScreenNavController<ScreenStructure>,
    private val globalEventSender: EventSender<GlobalEvent>,
    private val coroutineScope: CoroutineScope,
) : LoginCheckUseCaseImpl.EventListener {
    override fun error(message: String) {
        coroutineScope.launch {
            globalEventSender.send {
                it.showSnackBar(message)
            }
        }
    }

    override fun logout() {
        navController.navigate(ScreenStructure.Login)
    }
}