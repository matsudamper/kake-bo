package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily

@Immutable
public interface RootScreenScaffoldListener {
    public fun onClickHome()
    public fun onClickRegister()
    public fun onClickSettings()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RootScreenScaffold(
    modifier: Modifier = Modifier,
    currentScreen: Screen.Root,
    listener: RootScreenScaffoldListener,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val isLargeScreen by remember {
            derivedStateOf {
                maxWidth > 800.dp
            }
        }
        Scaffold(
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                KakeBoTopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = {
                        Text("家計簿")
                    }
                )
            },
            bottomBar = {
                if (isLargeScreen.not()) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentScreen is Screen.Root.Home,
                            onClick = { listener.onClickHome() },
                            icon = {
                                Icon(Icons.Default.Settings, "")
                            },
                            label = {
                                Text(
                                    "Home",
                                    fontFamily = rememberCustomFontFamily(),
                                )
                            },
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Root.Register,
                            onClick = { listener.onClickRegister() },
                            icon = {
                                Icon(Icons.Default.Add, "")
                            },
                            label = {
                                Text(
                                    "登録",
                                    fontFamily = rememberCustomFontFamily(),
                                )
                            },
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Root.Settings,
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
                            selected = currentScreen is Screen.Root.Home,
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
                            selected = currentScreen is Screen.Root.Register,
                            onClick = { listener.onClickRegister() },
                            icon = {
                                Icon(Icons.Default.Add, "")
                            },
                            label = {
                                Text(
                                    "登録",
                                    fontFamily = rememberCustomFontFamily(),
                                )
                            },
                        )
                        NavigationRailItem(
                            selected = currentScreen is Screen.Root.Settings,
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
                Box {
                    content()
                }
            }
        }
    }
}
