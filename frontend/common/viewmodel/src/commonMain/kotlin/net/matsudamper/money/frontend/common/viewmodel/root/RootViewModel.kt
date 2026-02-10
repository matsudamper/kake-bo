package net.matsudamper.money.frontend.common.viewmodel.root

import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectFeature
import net.matsudamper.money.frontend.common.viewmodel.CommonViewModel
import net.matsudamper.money.frontend.common.viewmodel.GlobalEventHandlerLoginCheckUseCaseDelegate

public class RootViewModel(
    private val loginCheckUseCase: GlobalEventHandlerLoginCheckUseCaseDelegate,
    scopedObjectFeature: ScopedObjectFeature,
) : CommonViewModel(scopedObjectFeature) {
    public fun navigateChanged() {
        viewModelScope.launch {
            loginCheckUseCase.check()
        }
    }
}
