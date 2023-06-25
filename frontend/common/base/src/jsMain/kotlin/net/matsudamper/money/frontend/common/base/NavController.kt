package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.browser.window

@Immutable
public class ScreenNavControllerImpl(
    initial: Screen,
    directions: List<Screen>,
) : ScreenNavController {
    override var currentNavigation: Screen by mutableStateOf<Screen>(initial)

    init {
        currentNavigation = directions.first { it.url == window.location.pathname }

        window.addEventListener(
            "popstate",
            callback = {
                currentNavigation = directions.first { it.url == window.location.pathname }
            },
        )
    }

    override fun navigate(navigation: Screen) {
        window.history.pushState(
            data = null,
            title = navigation.title,
            url = navigation.url,
        )
        println("navigate: $navigation")
        currentNavigation = navigation
    }
}
