package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import kotlinx.browser.window

@Stable
internal class ScreenNavControllerImpl(
    private val initial: IScreenStructure,
    private val currentScreenStructureProvider: () -> IScreenStructure,
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
            val item = backstackEntries.last()
            return ScreenNavController.NavStackEntry(
                structure = item.structure,
                isHome = item.isHome,
                savedState = false,
            )
        }
    override val canGoBack: Boolean = true

    init {
        updateScreenState(
            currentScreenStructureProvider(),
        )

        window.addEventListener(
            "popstate",
            callback = {
                updateScreenState(
                    currentScreenStructureProvider(),
                )
            },
        )
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
                window.history.pushState(
                    data = null,
                    title = backstackEntries.last().structure.direction.title,
                    url = backstackEntries.last().structure.createUrl(),
                )
                return
            }
        }

        val url = navigation.createUrl()
        if (backstackEntries.last().structure.equalScreen(navigation)) {
            window.history.replaceState(
                data = null,
                title = navigation.direction.title,
                url = url,
            )
        } else {
            window.history.pushState(
                data = null,
                title = navigation.direction.title,
                url = url,
            )
        }
        updateScreenState(navigation, savedState = savedState, isRoot = isRoot)
    }

    override fun navigateToHome() {
        while (backstackEntries.isNotEmpty()) {
            if (backstackEntries.last().isHome) {
                break
            }
            back()
        }
        navigate(
            backstackEntries.lastOrNull()?.structure ?: initial,
        )
    }

    override fun back() {
        window.history.back()
    }

    private fun updateScreenState(screenStructure: IScreenStructure, savedState: Boolean = false, isRoot: Boolean = false) {
        /**
         * JSの場合はキャンセルさせず、上に積む。ブラウザの履歴のハンドリングが大変なので
         */
        backstackEntries = backstackEntries.toMutableStateList().also {
            it.add(
                ScreenNavController.NavStackEntry(
                    structure = screenStructure,
                    isHome = when (screenStructure) {
                        is ScreenStructure.Root -> true
                        else -> false
                    },
                    savedState = savedState,
                ),
            )
        }
    }
}
