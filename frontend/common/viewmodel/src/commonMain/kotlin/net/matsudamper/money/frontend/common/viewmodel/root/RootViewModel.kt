package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate

public class RootViewModel(
    private val loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    private val navController: ScreenNavController,
    scopedObjectFeature: ScopedObjectFeature,
) : CommonViewModel(scopedObjectFeature) {
    public fun navigateChanged() {
        viewModelScope.launch {
            if (loginCheckUseCase.check().not()) {
                navController.navigate(ScreenStructure.Login)
            }
        }
    }
}
