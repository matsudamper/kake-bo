package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import org.w3c.dom.PopStateEvent

@Stable
internal class ScreenNavControllerImpl(
    private val initial: IScreenStructure,
    private val currentScreenStructureProvider: () -> IScreenStructure,
) : ScreenNavController {
    private val navBackstack = BrowserNavBackstack(initial)
    private var currentHistoryIndex = 0

    override val savedScopeKeys: Set<String>
        get() = backstackEntries.map { it.scopeKey }.toSet()
    override var backstackEntries: List<IScreenStructure> by mutableStateOf(listOf(initial))
        private set
    override val currentBackstackEntry: IScreenStructure?
        get() {
            return backstackEntries.lastOrNull()
        }
    override val canGoBack: Boolean = true

    init {
        window.history.replaceState(
            data = createHistoryState(0),
            title = "",
            url = window.location.href,
        )
        navBackstack.addScreen(currentScreenStructureProvider())
        backstackEntries = navBackstack.entries

        window.addEventListener(
            "popstate",
            callback = { event ->
                val popStateEvent = event.unsafeCast<PopStateEvent>()
                val newIndex = readHistoryIndex(popStateEvent.state)
                val delta = newIndex - currentHistoryIndex
                currentHistoryIndex = newIndex
                navBackstack.handlePopState(delta, currentScreenStructureProvider())
                backstackEntries = navBackstack.entries
            },
        )
    }

    override fun navigateReplace(navigation: IScreenStructure) {
        val url = navigation.createUrl()
        window.history.replaceState(
            data = createHistoryState(currentHistoryIndex),
            title = navigation.direction.title,
            url = url,
        )
        navBackstack.navigateReplace(navigation)
        backstackEntries = navBackstack.entries
    }

    override fun navigate(navigation: IScreenStructure, savedState: Boolean) {
        val result = navBackstack.navigate(navigation)
        backstackEntries = navBackstack.entries
        when (result) {
            is BrowserNavBackstack.NavigationResult.Push -> {
                currentHistoryIndex++
                window.history.pushState(
                    data = createHistoryState(currentHistoryIndex),
                    title = result.screen.direction.title,
                    url = result.screen.createUrl(),
                )
            }
            is BrowserNavBackstack.NavigationResult.Replace -> {
                window.history.replaceState(
                    data = createHistoryState(currentHistoryIndex),
                    title = result.screen.direction.title,
                    url = result.screen.createUrl(),
                )
            }
        }
    }

    override fun navigateToHome() {
        val steps = navBackstack.stepsToHome()
        if (steps < 0) {
            window.history.go(steps)
        }
    }

    override fun back() {
        window.history.back()
    }

    private fun createHistoryState(index: Int): Any {
        val obj = js("({})")
        obj.index = index
        return obj
    }

    private fun readHistoryIndex(state: Any?): Int {
        if (state == null) return 0
        return (state.asDynamic().index as? Double)?.toInt() ?: 0
    }
}
