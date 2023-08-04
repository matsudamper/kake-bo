package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Immutable

@Immutable
public interface ScreenNavController<D: IScreenStructure<D>> {
    public val currentNavigation: D
    public fun back()
    public fun navigate(
        navigation: D,
    )

    public fun navigateToHome()
}

public interface DirectionTitle {
    public val title: String
}

public interface DirectionUrl {
    public val placeholderUrl: String
}

public interface Direction : DirectionTitle, DirectionUrl
