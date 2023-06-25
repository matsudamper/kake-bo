package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Immutable

@Immutable
public interface ScreenNavController {
    public var currentNavigation: Screen
    public fun navigate(navigation: Screen)
}

public interface Direction {
    public val title: String
    public val url: String
}
