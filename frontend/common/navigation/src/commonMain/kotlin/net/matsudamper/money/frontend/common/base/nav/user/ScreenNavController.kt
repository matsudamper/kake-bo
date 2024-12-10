package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
public interface ScreenNavController {
    public val backstackEntries: List<NavStackEntry>
    public val currentBackstackEntry: NavStackEntry

    public val canGoBack: Boolean

    public fun back()

    public fun navigate(navigation: IScreenStructure)

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
public expect fun rememberMainScreenNavController(): ScreenNavController

public interface DirectionTitle {
    public val title: String
}

public interface DirectionUrl {
    public val placeholderUrl: String
}

public interface Direction : DirectionTitle, DirectionUrl
