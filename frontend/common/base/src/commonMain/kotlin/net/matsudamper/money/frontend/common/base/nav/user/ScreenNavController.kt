package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Immutable

@Immutable
public interface ScreenNavController {
    public val currentNavigation: ScreenStructure
    public fun back()
    public fun <T : ScreenStructure> navigate(
        navigation: T,
        urlBuilder: (T) -> String = { it.direction.url },
    )
}

public interface Direction {
    public val title: String
    public val url: String

    public fun createUrl(param: Map<String, String>): String = url
    public fun parseArgument(path: String): Map<String, String> = mapOf()
}
