package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList

@Composable
public actual fun rememberMainScreenNavController(initial: IScreenStructure): ScreenNavController {
    return rememberSaveable(saver = ScreenNavControllerImpl.Saver(currentCompositeKeyHash)) {
        ScreenNavControllerImpl(initial)
    }
}

@Stable
internal class ScreenNavControllerImpl(
    initial: IScreenStructure,
) : ScreenNavController {
    private var savedScopeKeyWithIsSavedList: Set<IScreenStructure> by mutableStateOf(setOf())
    override val savedScopeKeys: Set<String>
        get() = savedScopeKeyWithIsSavedList
            .plus(backstackEntries)
            .map { it.sameScreenId }
            .toSet()
    override var backstackEntries: List<IScreenStructure> by mutableStateOf(listOf(initial))
    override val currentBackstackEntry: IScreenStructure
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
            if (backstackEntries.last() is ScreenStructure.Root) {
                break
            }
            back()
        }
    }

    override fun navigate(navigation: IScreenStructure, savedState: Boolean) {
        println("${backstackEntries.map { it.direction.title }} -> ${navigation.direction.title}")
        if (navigation.stackGroupId != null && navigation.stackGroupId != currentBackstackEntry.stackGroupId) {
            val targetGroupTailIndex = backstackEntries.indexOfLast { it.stackGroupId == navigation.stackGroupId }
                .takeIf { it >= 0 }
                ?.plus(1)

            if (targetGroupTailIndex != null) {
                val targetGroupStartIndex = backstackEntries.take(targetGroupTailIndex)
                    .indexOfLast { it.stackGroupId != navigation.stackGroupId }
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
        backstackEntries = backstackEntries.plus(navigation)
        if (savedState) {
            savedScopeKeyWithIsSavedList += navigation
        }
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
