package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember

public typealias MainScreenNavController = ScreenNavController<ScreenStructure>

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
internal expect class ScreenNavControllerImpl(
    initial: ScreenStructure,
) : MainScreenNavController

@Composable
public fun rememberMainScreenNavController(): MainScreenNavController {
    return remember { ScreenNavControllerImpl(RootHomeScreenStructure.Home) }
}

public interface DirectionTitle {
    public val title: String
}

public interface DirectionUrl {
    public val placeholderUrl: String
}

public interface Direction : DirectionTitle, DirectionUrl
