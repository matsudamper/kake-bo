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
        AndroidScreenNavControllerImpl(
            navBackstack = navBackstack as NavBackStack<IScreenStructure>,
        )
    }
}

private class AndroidScreenNavControllerImpl(
    private val navBackstack: NavBackStack<IScreenStructure>,
) : ScreenNavController {
    override val savedScopeKeys: Set<String>
        get() = navBackstack.map {
            it.scopeKey
        }.toSet()
    override val backstackEntries: List<IScreenStructure> get() = navBackstack.toList()
    override val currentBackstackEntry: IScreenStructure? get() = navBackstack.lastOrNull()
    override val canGoBack: Boolean get() = navBackstack.size > 1

    override fun back() {
        navBackstack.removeAt(navBackstack.lastIndex)
    }

    override fun navigateReplace(navigation: IScreenStructure) {
        navBackstack.removeAt(navBackstack.lastIndex)
        navBackstack.add(navigation)
    }

    override fun navigate(navigation: IScreenStructure, savedState: Boolean) {
        if (navigation.stackGroupId != null && navigation.stackGroupId != currentBackstackEntry?.stackGroupId) {
            val targetGroupTailIndex = navBackstack.indexOfLast { it.stackGroupId == navigation.stackGroupId }
                .takeIf { it >= 0 }
                ?.plus(1)

            if (targetGroupTailIndex != null) {
                val targetGroupStartIndex = navBackstack.take(targetGroupTailIndex)
                    .indexOfLast { it.stackGroupId != navigation.stackGroupId }
                    .plus(1)

                val targetRange = navBackstack.subList(targetGroupStartIndex, targetGroupTailIndex).toList()
                repeat(targetRange.size) {
                    navBackstack.removeAt(targetGroupStartIndex)
                }
                navBackstack.addAll(targetRange)

                return
            }
        }
        navBackstack.add(navigation)
    }

    override fun navigateToHome() {
        while (backstackEntries.isNotEmpty()) {
            if (backstackEntries.last() is ScreenStructure.Root) {
                break
            }
            back()
        }
    }
}
