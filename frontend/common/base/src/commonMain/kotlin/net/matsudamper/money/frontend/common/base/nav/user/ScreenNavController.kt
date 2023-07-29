package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Immutable

@Immutable
public interface ScreenNavController {
    public val currentNavigation: ScreenStructure
    public fun back()
    public fun <T : ScreenStructure> navigate(
        navigation: T,
    )
}

public interface Direction {
    public val title: String
    public val placeholderUrl: String
}
