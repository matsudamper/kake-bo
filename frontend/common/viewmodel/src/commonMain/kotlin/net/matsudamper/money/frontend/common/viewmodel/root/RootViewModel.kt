package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavControllerImpl
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.viewmodel.LoginCheckUseCase

public class RootViewModel(
    private val loginCheckUseCase: LoginCheckUseCase,
    private val coroutineScope: CoroutineScope,
    private val navController: ScreenNavControllerImpl,
) {
    public fun navigateChanged() {
        coroutineScope.launch {
            if (loginCheckUseCase.check().not()) {
                navController.navigate(ScreenStructure.Login)
            }
        }
    }
}
