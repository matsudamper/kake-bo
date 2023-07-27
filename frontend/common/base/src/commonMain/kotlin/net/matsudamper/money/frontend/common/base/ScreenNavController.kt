package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable

@Immutable
public interface ScreenNavController {
    public val currentNavigation: Screen
    public fun back()
    public fun <T : Screen> navigate(navigation: T, urlBuilder: (T) -> String = { it.url })
}

public interface Direction {
    public val title: String
    public val url: String

    public fun parseArgument(path: String): Map<String, String> = mapOf()
}
