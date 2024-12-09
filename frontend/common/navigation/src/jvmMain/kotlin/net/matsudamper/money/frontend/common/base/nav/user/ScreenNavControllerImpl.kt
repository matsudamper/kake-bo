package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
internal class ScreenNavControllerImpl(
    initial: ScreenStructure,
) : ScreenNavController<IScreenStructure> {
    private var internalCurrentNavigation: List<IScreenStructure> by mutableStateOf(mutableListOf(initial))
    override val currentNavigation: IScreenStructure get() = internalCurrentNavigation.last()

    public override val canGoBack: Boolean get() = internalCurrentNavigation.size > 1

    override fun back() {
        val dropTarget = internalCurrentNavigation.dropLast(1)
        internalCurrentNavigation = dropTarget.toMutableList()
    }

    override fun navigateToHome() {
        while (internalCurrentNavigation.isNotEmpty()) {
            when (internalCurrentNavigation.last()) {
                is ScreenStructure.Root -> {
                    break
                }
                else -> back()
            }
        }
    }

    override fun navigate(navigation: IScreenStructure) {
        internalCurrentNavigation = internalCurrentNavigation.plus(navigation)
    }
}
