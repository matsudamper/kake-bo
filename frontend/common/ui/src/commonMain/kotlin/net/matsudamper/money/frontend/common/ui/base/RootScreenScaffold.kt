package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.CustomColors
import net.matsudamper.money.frontend.common.ui.LocalIsLargeScreen
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener.Companion.previewImpl
import net.matsudamper.money.frontend.common.ui.lib.asWindowInsets
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Immutable
public interface RootScreenScaffoldListener {
    public val kakeboScaffoldListener: KakeboScaffoldListener

    public fun onClickHome()

    public fun onClickList()

    public fun onClickSettings()

    public fun onClickAdd()

    public companion object {
        internal val previewImpl = object : RootScreenScaffoldListener {
            override fun onClickAdd() {}
            override fun onClickSettings() {}
            override fun onClickHome() {}
            override fun onClickList() {}
            override val kakeboScaffoldListener: KakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {}
            }
        }
    }
}

public enum class RootScreenTab {
    Home,
    List,
    Add,
    Settings,
}

public val PreviewSharedNavigation: SharedNavigation = SharedNavigation()

@Stable
public class SharedNavigation {
    public var windowInsets: PaddingValues by mutableStateOf(PaddingValues())
    public var currentScreen: RootScreenTab by mutableStateOf(RootScreenTab.Home)
    public var listener: RootScreenScaffoldListener by mutableStateOf(previewImpl)

    public val Bottom: @Composable (() -> Unit) = movableContentOf {
        NavigationBar(
            windowInsets = windowInsets.asWindowInsets()
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
        ) {
            NavigationBarItem(
                selected = currentScreen == RootScreenTab.Home,
                onClick = { listener.onClickHome() },
                icon = {
                    Icon(Icons.Default.Home, null)
                },
                label = {
                    Text(
                        "Home",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            NavigationBarItem(
                selected = currentScreen == RootScreenTab.List,
                onClick = { listener.onClickList() },
                icon = {
                    Icon(Icons.AutoMirrored.Filled.List, null)
                },
                label = {
                    Text(
                        "一覧",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            NavigationBarItem(
                selected = currentScreen == RootScreenTab.Add,
                onClick = { listener.onClickAdd() },
                icon = {
                    Icon(Icons.Default.Add, null)
                },
                label = {
                    Text(
                        "追加",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            NavigationBarItem(
                selected = currentScreen == RootScreenTab.Settings,
                onClick = { listener.onClickSettings() },
                icon = {
                    Icon(Icons.Default.Settings, null)
                },
                label = {
                    Text(
                        "設定",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
        }
    }

    public val Rail: @Composable (() -> Unit) = movableContentOf {
        NavigationRail(
            modifier = Modifier.padding(top = 8.dp),
        ) {
            NavigationRailItem(
                selected = currentScreen == RootScreenTab.Home,
                onClick = { listener.onClickHome() },
                icon = {
                    Icon(Icons.Default.Home, null)
                },
                label = {
                    Text(
                        "Home",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            NavigationRailItem(
                selected = currentScreen == RootScreenTab.List,
                onClick = { listener.onClickList() },
                icon = {
                    Icon(Icons.AutoMirrored.Filled.List, null)
                },
                label = {
                    Text(
                        "一覧",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            NavigationRailItem(
                selected = currentScreen == RootScreenTab.Add,
                onClick = { listener.onClickAdd() },
                icon = {
                    Icon(Icons.Default.Add, null)
                },
                label = {
                    Text(
                        "追加",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
            NavigationRailItem(
                selected = currentScreen == RootScreenTab.Settings,
                onClick = { listener.onClickSettings() },
                icon = {
                    Icon(Icons.Default.Settings, null)
                },
                label = {
                    Text(
                        "設定",
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
            )
        }
    }
}

@Composable
internal fun RootScreenScaffold(
    modifier: Modifier = Modifier,
    currentScreen: RootScreenTab,
    topBar: @Composable () -> Unit,
    windowInsets: PaddingValues,
    listener: RootScreenScaffoldListener,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    navigationUi: SharedNavigation,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        KakeboScaffold(
            modifier = Modifier.fillMaxWidth(),
            topBar = topBar,
            windowInsets = windowInsets,
            snackbarHost = {
                MySnackBarHost(
                    hostState = snackbarHostState,
                )
            },
            bottomBar = {
                if (LocalIsLargeScreen.current.not()) {
                    navigationUi.Bottom()
                }
            },
        ) {
            Row(
                modifier = Modifier.padding(it),
            ) {
                if (LocalIsLargeScreen.current) {
                    navigationUi.Rail()
                    VerticalDivider(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(),
                        color = CustomColors.MenuDividerColor,
                    )
                }
                Box {
                    content()
                }
            }
        }
    }
}
