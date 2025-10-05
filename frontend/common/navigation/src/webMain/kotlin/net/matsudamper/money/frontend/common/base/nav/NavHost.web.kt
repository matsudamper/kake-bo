package net.matsudamper.money.frontend.common.base.nav

import androidx.compose.runtime.Composable
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController

@Composable
public actual fun NavHost(
    navController: ScreenNavController,
    content: @Composable ((ScreenNavController.NavStackEntry) -> Unit)
) {
    InternalNavHost(
        navController = navController,
        content = content
    )
}