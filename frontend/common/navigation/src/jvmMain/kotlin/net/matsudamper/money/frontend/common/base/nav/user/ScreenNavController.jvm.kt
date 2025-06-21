package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
public actual fun rememberMainScreenNavController(initial: IScreenStructure): ScreenNavController {
    return rememberSaveable(saver = ScreenNavControllerImpl.Saver(currentCompositeKeyHash)) {
        ScreenNavControllerImpl(
            initial = initial,
        )
    }
}
