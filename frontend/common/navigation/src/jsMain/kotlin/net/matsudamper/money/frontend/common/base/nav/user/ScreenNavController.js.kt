package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
public actual fun rememberMainScreenNavController(): ScreenNavController<ScreenStructure> {
    return remember { ScreenNavControllerImpl(initial = RootHomeScreenStructure.Home) }
}
