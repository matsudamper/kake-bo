package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window

@Immutable
public class ScreenNavControllerImpl(
    initial: Screen,
    private val directions: List<Screen>,
) : ScreenNavController {
    override var currentNavigation: Screen by mutableStateOf<Screen>(initial)

    init {
        updateNavigation()

        window.addEventListener(
            "popstate",
            callback = { updateNavigation() },
        )
    }

    private fun updateNavigation() {
        currentNavigation = directions.first { it.url == window.location.pathname }
    }

    override fun navigate(navigation: Screen) {
        window.history.pushState(
            data = TAG,
            title = navigation.title,
            url = navigation.url,
        )
        println("navigate: $navigation")
        currentNavigation = navigation
    }

    override fun back() {
        window.history.back()
    }

    public companion object {
        private const val TAG = "FHAOHWO!!O@&*DAOH(GA&&(DA&("
    }
}
