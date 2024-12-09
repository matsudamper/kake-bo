package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
public interface ScreenNavController<D : IScreenStructure<D>> {
    public val currentNavigation: D

    public val canGoBack: Boolean

    public fun back()

    public fun navigate(navigation: D)

    public fun navigateToHome()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@Stable
internal expect class ScreenNavControllerImpl(
    initial: ScreenStructure,
) : ScreenNavController<ScreenStructure>

@Composable
public fun rememberMainScreenNavController(): ScreenNavController<ScreenStructure> {
    return remember { ScreenNavControllerImpl(RootHomeScreenStructure.Home) }
}

public interface DirectionTitle {
    public val title: String
}

public interface DirectionUrl {
    public val placeholderUrl: String
}

public interface Direction : DirectionTitle, DirectionUrl
