package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
public interface ScreenNavController {
    /**
     * 生きている保存するべきID
     */
    public val savedScopeKeys: Set<String>
    public val backstackEntries: List<IScreenStructure>
    public val currentBackstackEntry: IScreenStructure

    public val canGoBack: Boolean

    public fun back()

    public fun navigate(
        navigation: IScreenStructure,
        savedState: Boolean = false,
    )

    public fun navigateToHome()

    public interface RemovedBackstackEntryListener {
        public fun onRemoved(entry: IScreenStructure)
    }
}

@Composable
public expect fun rememberMainScreenNavController(initial: IScreenStructure): ScreenNavController

public interface DirectionTitle {
    public val title: String
}

public interface DirectionUrl {
    public val placeholderUrl: String
}

public interface Direction : DirectionTitle, DirectionUrl
