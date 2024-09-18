package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Immutable
public actual class ScreenNavControllerImpl actual constructor(
    initial: ScreenStructure,
) : ScreenNavController<ScreenStructure> {
    private var internalCurrentNavigation: ScreenStructure by mutableStateOf(initial)
    override val currentNavigation: ScreenStructure get() = internalCurrentNavigation

    override fun back() {
        // TODO
    }

    override fun navigateToHome() {
        // TODO
    }

    override fun navigate(navigation: ScreenStructure) {
        internalCurrentNavigation = navigation
    }
}
