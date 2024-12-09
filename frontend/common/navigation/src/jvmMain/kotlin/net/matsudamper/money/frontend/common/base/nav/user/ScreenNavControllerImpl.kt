package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue

@Stable
internal class ScreenNavControllerImpl(
    initial: ScreenStructure,
    private val savedStateHolder: SaveableStateHolder,
) : ScreenNavController {
    private var internalCurrentNavigation: List<ScreenNavController.NavStackEntry> by mutableStateOf(
        mutableListOf(
            ScreenNavController.NavStackEntry(
                structure = initial,
                isHome = true,
            ),
        ),
    )
    override val currentBackstackEntry: ScreenNavController.NavStackEntry get() = internalCurrentNavigation.last()

    public override val canGoBack: Boolean get() = internalCurrentNavigation.size > 1

    override fun back() {
        val dropTarget = internalCurrentNavigation.dropLast(1)
        internalCurrentNavigation = dropTarget.toMutableList()
    }

    override fun navigateToHome() {
        while (internalCurrentNavigation.isNotEmpty()) {
            if (internalCurrentNavigation.last().isHome) {
                break
            }
            back()
        }
    }

    override fun navigate(navigation: IScreenStructure) {
        internalCurrentNavigation = internalCurrentNavigation.plus(
            ScreenNavController.NavStackEntry(
                structure = navigation,
                isHome = navigation is ScreenStructure.Root,
            ),
        )
    }
}
