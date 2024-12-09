package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Stable
internal actual class ScreenNavControllerImpl actual constructor(
    initial: ScreenStructure,
) : ScreenNavController<ScreenStructure> {
    private var internalCurrentNavigation: List<ScreenStructure> by mutableStateOf(mutableListOf(initial))
    override val currentNavigation: ScreenStructure get() = internalCurrentNavigation.last()

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

    override fun navigate(navigation: ScreenStructure) {
        internalCurrentNavigation = internalCurrentNavigation.plus(navigation)
    }
}
