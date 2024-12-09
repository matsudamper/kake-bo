package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
public interface ScreenNavController<D : IScreenStructure> {
    public val currentNavigation: D

    public val canGoBack: Boolean

    public fun back()

    public fun navigate(navigation: D)

    public fun navigateToHome()
}

@Composable
public expect fun rememberMainScreenNavController(): ScreenNavController<ScreenStructure>

public interface DirectionTitle {
    public val title: String
}

public interface DirectionUrl {
    public val placeholderUrl: String
}

public interface Direction : DirectionTitle, DirectionUrl
