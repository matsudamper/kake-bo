package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Immutable

@Immutable
public actual class ScreenNavControllerImpl actual constructor(
    initial: ScreenStructure,
) : ScreenNavController<ScreenStructure> {
    override val currentNavigation: ScreenStructure
        get() = TODO("Not yet implemented")

    override fun back() {
        TODO("Not yet implemented")
    }

    override fun navigateToHome() {
        TODO("Not yet implemented")
    }

    override fun navigate(navigation: ScreenStructure) {
        TODO("Not yet implemented")
    }
}
