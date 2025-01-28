package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

@Composable
public actual fun rememberMainScreenNavController(initial: IScreenStructure): ScreenNavController {
    val holder = rememberSaveableStateHolder()
    return remember(holder) {
        ScreenNavControllerImpl(
            initial = initial,
        )
    }
}
