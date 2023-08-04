package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

@Immutable
public interface RootScreenScaffoldListener {
    public fun onClickHome()
    public fun onClickRegister()
    public fun onClickSettings()
    public fun onClickMail()
}

public enum class RootScreenTab {
    Home,
    List,
    Mail,
    Settings,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RootScreenScaffold(
    modifier: Modifier = Modifier,
    currentScreen: RootScreenTab,
    listener: RootScreenScaffoldListener,
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
        Scaffold(
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                KakeBoTopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = {
                        Text(
                            text = "家計簿",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    },
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
                            onClick = { listener.onClickRegister() },
                            icon = {
                                Icon(Icons.Default.List, "")
                            },
                            label = {
                                Text(
                                    "リスト",
                                    fontFamily = rememberCustomFontFamily(),
                                )
                            },
                        )
                        NavigationBarItem(
                            selected = currentScreen == RootScreenTab.Mail,
                            onClick = { listener.onClickMail() },
                            icon = {
                                Icon(Icons.Default.Email, "")
                            },
                            label = {
                                Text(
                                    "メール",
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
                            onClick = { listener.onClickRegister() },
                            icon = {
                                Icon(Icons.Default.List, "")
                            },
                            label = {
                                Text(
                                    "リスト",
                                    fontFamily = rememberCustomFontFamily(),
                                )
                            },
                        )
                        NavigationRailItem(
                            selected = currentScreen == RootScreenTab.Mail,
                            onClick = { listener.onClickMail() },
                            icon = {
                                Icon(Icons.Default.Email, "")
                            },
                            label = {
                                Text(
                                    "メール",
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
                }
                Box {
                    content()
                }
            }
        }
    }
}
