package net.matsudamper.money.frontend.common.ui.layout.nav

import androidx.compose.runtime.Composable

@Composable
public fun <T> NavHost(
    navController: T,
    content: @Composable (T) -> Unit,
) {
    content(navController)
}
