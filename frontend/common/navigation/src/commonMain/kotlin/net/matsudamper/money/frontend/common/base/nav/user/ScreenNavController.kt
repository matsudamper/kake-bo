package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Immutable

@Immutable
public interface ScreenNavController<D : IScreenStructure<D>> {
    public val currentNavigation: D

    public val canGoBack: Boolean

    public fun back()

    public fun navigate(navigation: D)

    public fun navigateToHome()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Immutable
public expect class ScreenNavControllerImpl(
    initial: ScreenStructure,
) : ScreenNavController<ScreenStructure>

public interface DirectionTitle {
    public val title: String
}

public interface DirectionUrl {
    public val placeholderUrl: String
}

public interface Direction : DirectionTitle, DirectionUrl
