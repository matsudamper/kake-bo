package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack

@Composable
public actual fun rememberMainScreenNavController(initial: IScreenStructure): ScreenNavController {
    val navBackstack = rememberNavBackStack()
    return remember(navBackstack, initial) {
        navBackstack.add(initial)
        @Suppress("UNCHECKED_CAST")
        CommonScreenNavControllerImpl(
            navBackstack = navBackstack as NavBackStack<IScreenStructure>,
        )
    }
}
