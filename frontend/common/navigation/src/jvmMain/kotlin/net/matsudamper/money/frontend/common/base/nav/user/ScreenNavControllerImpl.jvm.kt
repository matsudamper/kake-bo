package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList

@Stable
internal class ScreenNavControllerImpl(
    initial: IScreenStructure,
) : ScreenNavController {
    override var backstackEntries: List<ScreenNavController.NavStackEntry> by mutableStateOf(
        listOf(
            ScreenNavController.NavStackEntry(
                structure = initial,
                isHome = true,
                savedState = false,
            ),
        ),
    )
    override val currentBackstackEntry: ScreenNavController.NavStackEntry
        get() {
            return backstackEntries.last()
        }

    public override val canGoBack: Boolean get() = backstackEntries.size > 1

    override fun back() {
        val dropTarget = backstackEntries.last()
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

    override fun navigate(navigation: IScreenStructure, savedState: Boolean, isRoot: Boolean) {
        println("${backstackEntries.map { it.structure.direction.title }} -> ${navigation.direction.title}")
        if (navigation.stackGroupId != null && navigation.stackGroupId != currentBackstackEntry.structure.stackGroupId) {
            val targetGroupTailIndex = backstackEntries.indexOfLast { it.structure.stackGroupId == navigation.stackGroupId }
                .takeIf { it >= 0 }
                ?.plus(1)

            if (targetGroupTailIndex != null) {
                val targetGroupStartIndex = backstackEntries.take(targetGroupTailIndex)
                    .indexOfLast { it.structure.stackGroupId != navigation.stackGroupId }
                    .plus(1)

                val list = backstackEntries.toMutableList()
                val targetRange = list.subList(targetGroupStartIndex, targetGroupTailIndex).toList()
                repeat(targetRange.size) {
                    list.removeAt(targetGroupStartIndex)
                }
                list.addAll(targetRange)

                backstackEntries = list
                return
            }
        }
        backstackEntries = backstackEntries.plus(
            ScreenNavController.NavStackEntry(
                structure = navigation,
                isHome = navigation is ScreenStructure.Root,
                savedState = savedState,
            ),
        )
    }

    companion object {
        private val globalSaver = mutableMapOf<Int, ScreenNavControllerImpl>()

        fun Saver(id: Int): Saver<ScreenNavControllerImpl, *> {
            return Saver(
                save = {
                    globalSaver[id] = it
                    ""
                },
                restore = {
                    globalSaver[id]
                },
            )
        }
    }
}
