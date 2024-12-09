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
    private val navViewModel: NavViewModel,
) : ScreenNavController {
    private val removedBackstackEntryListeners = mutableSetOf<ScreenNavController.RemovedBackstackEntryListener>()
    private var internalCurrentNavigation: List<InternalNavStackEntry> by mutableStateOf(
        mutableListOf(
            InternalNavStackEntry(
                structure = initial,
                isHome = true,
            ),
        ),
    )
    override val currentBackstackEntry: ScreenNavController.NavStackEntry
        get() {
            val item = internalCurrentNavigation.last()
            return ScreenNavController.NavStackEntry(
                structure = item.structure,
                isHome = item.isHome,
                scopedObjectStore = navViewModel.createOrGetScopedObjectStore(item.structure),
            )
        }

    public override val canGoBack: Boolean get() = internalCurrentNavigation.size > 1

    override fun back() {
        val dropTarget = internalCurrentNavigation.last()
        removedBackstackEntryListeners.forEach { t ->
            t.onRemoved(
                ScreenNavController.NavStackEntry(
                    structure = dropTarget.structure,
                    isHome = dropTarget.isHome,
                    scopedObjectStore = navViewModel.createOrGetScopedObjectStore(dropTarget.structure),
                ),
            )
        }
        internalCurrentNavigation = internalCurrentNavigation.toMutableStateList().also {
            it.remove(dropTarget)
        }
        val hasOtherBothStructure = internalCurrentNavigation.any { it.structure == dropTarget.structure }
        if (hasOtherBothStructure.not()) {
            navViewModel.removeScopedObjectStore(dropTarget.structure)
        }
    }

    override fun navigateToHome() {
        while (internalCurrentNavigation.isNotEmpty()) {
            if (internalCurrentNavigation.last().isHome) {
                break
            }
            back()
        }
    }

    override fun addRemovedBackstackEntryListener(listener: ScreenNavController.RemovedBackstackEntryListener) {
        removedBackstackEntryListeners.add(listener)
    }

    override fun removeRemovedBackstackEntryListener(listener: ScreenNavController.RemovedBackstackEntryListener) {
        removedBackstackEntryListeners.remove(listener)
    }

    override fun navigate(navigation: IScreenStructure) {
        internalCurrentNavigation = internalCurrentNavigation.plus(
            InternalNavStackEntry(
                structure = navigation,
                isHome = navigation is ScreenStructure.Root,
            ),
        )
    }

    private data class InternalNavStackEntry(
        val structure: IScreenStructure,
        val isHome: Boolean,
    )
}
