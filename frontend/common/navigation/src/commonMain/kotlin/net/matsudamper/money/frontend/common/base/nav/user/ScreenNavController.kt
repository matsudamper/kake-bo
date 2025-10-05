package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
public interface ScreenNavController {
    /**
     * 生きている保存するべきID
     */
    public val savedScopeKeys: Set<String>
    public val backstackEntries: List<NavStackEntry>
    public val currentBackstackEntry: NavStackEntry

    public val canGoBack: Boolean

    public fun back()

    public fun navigate(
        navigation: IScreenStructure,
        savedState: Boolean = false,
        isRoot: Boolean = false,
    )

    public fun navigateToHome()

    public data class NavStackEntry(
        val structure: IScreenStructure,
        val isHome: Boolean,
    )

    public interface RemovedBackstackEntryListener {
        public fun onRemoved(entry: NavStackEntry)
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
