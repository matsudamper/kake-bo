package net.matsudamper.money.frontend.common.base.nav.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import net.matsudamper.money.frontend.common.base.nav.ScopedObjectStore

@Stable
public interface ScreenNavController {
    public val currentBackstackEntry: NavStackEntry

    public val canGoBack: Boolean

    public fun back()

    public fun navigate(navigation: IScreenStructure)

    public fun navigateToHome()

    public fun addRemovedBackstackEntryListener(listener: RemovedBackstackEntryListener)

    public fun removeRemovedBackstackEntryListener(listener: RemovedBackstackEntryListener)

    public data class NavStackEntry(
        val structure: IScreenStructure,
        val isHome: Boolean,
        val scopedObjectStore: ScopedObjectStore,
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
