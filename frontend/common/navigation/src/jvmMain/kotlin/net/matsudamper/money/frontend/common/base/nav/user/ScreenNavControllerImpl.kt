package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Immutable
internal actual class ScreenNavControllerImpl actual constructor(
    initial: ScreenStructure,
) : MainScreenNavController {
    private var internalCurrentNavigation: List<ScreenStructure> by mutableStateOf(mutableListOf(initial))
    override val currentNavigation: ScreenStructure get() = internalCurrentNavigation.last()

    public override val canGoBack: Boolean get() = internalCurrentNavigation.size > 1

    override fun back() {
        internalCurrentNavigation = internalCurrentNavigation.dropLast(1).toMutableList()
    }

    override fun navigateToHome() {
        // TODO
    }

    override fun navigate(navigation: ScreenStructure) {
        internalCurrentNavigation = internalCurrentNavigation.plus(navigation)
    }
}
