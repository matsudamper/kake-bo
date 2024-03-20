package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.CustomColors
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Immutable
public interface RootScreenScaffoldListener {
    public val kakeboScaffoldListener: KakeboScaffoldListener

    public fun onClickHome()

    public fun onClickList()

    public fun onClickSettings()

    public fun onClickAdd()
}

public enum class RootScreenTab {
    Home,
    List,
    Add,
    Settings,
}

@Composable
internal fun RootScreenScaffold(
    modifier: Modifier = Modifier,
    currentScreen: RootScreenTab,
    topBar: @Composable () -> Unit,
    listener: RootScreenScaffoldListener,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val maxWidth by rememberUpdatedState(maxWidth)
        val isLargeScreen by remember {
            derivedStateOf {
                maxWidth > 800.dp
            }
        }
        KakeboScaffold(
            modifier = Modifier.fillMaxWidth(),
            topBar = topBar,
            snackbarHost = {
                MySnackBarHost(
                    hostState = snackbarHostState,
                )
            },
            bottomBar = {
                if (isLargeScreen.not()) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentScreen == RootScreenTab.Home,
                            onClick = { listener.onClickHome() },
                            icon = {
                                Icon(Icons.Default.Home, "")
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
                                Icon(Icons.Default.List, "")
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
                                Icon(Icons.Default.Add, "")
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
                                Icon(Icons.Default.Settings, "")
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
            },
        ) {
            Row(
                modifier = Modifier.padding(it),
            ) {
                if (isLargeScreen) {
                    NavigationRail(
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        NavigationRailItem(
                            selected = currentScreen == RootScreenTab.Home,
                            onClick = { listener.onClickHome() },
                            icon = {
                                Icon(Icons.Default.Home, "")
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
                                Icon(Icons.AutoMirrored.Filled.List, "")
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
                                Icon(Icons.Default.Add, "")
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
                                Icon(Icons.Default.Settings, "")
                            },
                            label = {
                                Text(
                                    "設定",
                                    fontFamily = rememberCustomFontFamily(),
                                )
                            },
                        )
                    }
                    VerticalDivider(
                        modifier =
                        Modifier
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
