package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList

@Stable
internal class ScreenNavControllerImpl(
    initial: ScreenStructure,
    private val savedStateHolder: SaveableStateHolder,
) : ScreenNavController {
    private val removedBackstackEntryListeners = mutableSetOf<ScreenNavController.RemovedBackstackEntryListener>()
    override var backstackEntries: List<ScreenNavController.NavStackEntry> by mutableStateOf(
        listOf(
            ScreenNavController.NavStackEntry(
                structure = initial,
                isHome = true,
            ),
        ),
    )
    override val currentBackstackEntry: ScreenNavController.NavStackEntry
        get() {
            val item = backstackEntries.last()
            return ScreenNavController.NavStackEntry(
                structure = item.structure,
                isHome = item.isHome,
            )
        }

    public override val canGoBack: Boolean get() = backstackEntries.size > 1

    override fun back() {
        val dropTarget = backstackEntries.last()
        removedBackstackEntryListeners.forEach { t ->
            t.onRemoved(
                ScreenNavController.NavStackEntry(
                    structure = dropTarget.structure,
                    isHome = dropTarget.isHome,
                ),
            )
        }
        backstackEntries = backstackEntries.toMutableStateList().also {
            it.remove(dropTarget)
        }
    }

    override fun navigateToHome() {
        while (backstackEntries.isNotEmpty()) {
            if (backstackEntries.last().isHome) {
                break
            }
            back()
        }
    }

    override fun navigate(navigation: IScreenStructure) {
        backstackEntries = backstackEntries.plus(
            ScreenNavController.NavStackEntry(
                structure = navigation,
                isHome = navigation is ScreenStructure.Root,
            ),
        )
    }
}
