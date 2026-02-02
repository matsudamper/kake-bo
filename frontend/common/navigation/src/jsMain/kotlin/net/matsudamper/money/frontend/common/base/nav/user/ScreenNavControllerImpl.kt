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
    override val savedScopeKeys: Set<String> = setOf()
    override var backstackEntries: List<IScreenStructure> by mutableStateOf(listOf(initial))
    override val currentBackstackEntry: IScreenStructure?
        get() {
            return backstackEntries.lastOrNull()
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
                window.history.pushState(
                    data = null,
                    title = backstackEntries.last().direction.title,
                    url = backstackEntries.last().createUrl(),
                )
                return
            }
        }

        val url = navigation.createUrl()
        if (backstackEntries.last().sameScreenId == navigation.sameScreenId) {
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
        updateScreenState(navigation)
    }

    override fun navigateToHome() {
        while (backstackEntries.isNotEmpty()) {
            if (backstackEntries.last() is ScreenStructure.Root) {
                break
            }
            back()
        }
        navigate(
            backstackEntries.lastOrNull() ?: initial,
        )
    }

    override fun back() {
        window.history.back()
    }

    private fun updateScreenState(screenStructure: IScreenStructure) {
        /**
         * JSの場合はキャンセルさせず、上に積む。ブラウザの履歴のハンドリングが大変なので
         */
        backstackEntries = backstackEntries.toMutableStateList().also {
            it.add(screenStructure)
        }
    }
}
