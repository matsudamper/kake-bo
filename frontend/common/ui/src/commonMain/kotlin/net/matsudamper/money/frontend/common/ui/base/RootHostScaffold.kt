package net.matsudamper.money.frontend.common.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.nav.EntryDecorator
import net.matsudamper.money.frontend.common.base.nav.user.IScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.RootHomeScreenStructure
import net.matsudamper.money.frontend.common.base.nav.user.ScreenNavController
import net.matsudamper.money.frontend.common.base.nav.user.ScreenStructure
import net.matsudamper.money.frontend.common.ui.CustomColors
import net.matsudamper.money.frontend.common.ui.LocalIsLargeScreen
import net.matsudamper.money.frontend.common.ui.lib.asWindowInsets
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

public val LocalRootScaffoldPadding: ProvidableCompositionLocal<PaddingValues> =
    staticCompositionLocalOf { PaddingValues(0.dp) }

public fun rootHostScaffoldEntryDecorator(
    navController: ScreenNavController,
): EntryDecorator {
    return EntryDecorator { structure, content ->
        if (structure is ScreenStructure.Root) {
            val windowInsets = WindowInsets.safeDrawing.asPaddingValues()
            val scrollToTopHandler = remember { ScrollToTopHandler() }
            val tab = structure.toRootScreenTab()
            val onClickTab: (RootScreenTab) -> Unit = { clickedTab ->
                if (clickedTab == tab) {
                    if (scrollToTopHandler.requestScrollToTop().not()) {
                        navController.navigate(clickedTab.toHomeScreenStructure())
                    }
                } else {
                    navController.navigate(clickedTab.toHomeScreenStructure())
                }
            }
            CompositionLocalProvider(LocalScrollToTopHandler provides scrollToTopHandler) {
                RootHostScaffoldContent(
                    currentScreen = tab,
                    onClickTab = onClickTab,
                    windowInsets = windowInsets,
                    modifier = Modifier.fillMaxSize(),
                ) { adjustedPadding ->
                    CompositionLocalProvider(LocalRootScaffoldPadding provides adjustedPadding) {
                        content()
                    }
                }
            }
        } else {
            content()
        }
    }
}

private fun ScreenStructure.Root.toRootScreenTab(): RootScreenTab {
    return when (this) {
        is RootHomeScreenStructure -> RootScreenTab.Home
        is ScreenStructure.Root.Add -> RootScreenTab.Add
        is ScreenStructure.Root.Settings -> RootScreenTab.Settings
        is ScreenStructure.Root.Usage -> RootScreenTab.List
    }
}

private fun RootScreenTab.toHomeScreenStructure(): IScreenStructure {
    return when (this) {
        RootScreenTab.Home -> RootHomeScreenStructure.Home
        RootScreenTab.List -> ScreenStructure.Root.Usage.Calendar()
        RootScreenTab.Add -> ScreenStructure.Root.Add.Root
        RootScreenTab.Settings -> ScreenStructure.Root.Settings.Root
    }
}

@Composable
private fun RootHostScaffoldContent(
    currentScreen: RootScreenTab,
    onClickTab: (RootScreenTab) -> Unit,
    windowInsets: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            if (LocalIsLargeScreen.current) {
                NavigationRail(
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    NavigationRailItem(
                        selected = currentScreen == RootScreenTab.Home,
                        onClick = { onClickTab(RootScreenTab.Home) },
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
                        onClick = { onClickTab(RootScreenTab.List) },
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
                        onClick = { onClickTab(RootScreenTab.Add) },
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
                        onClick = { onClickTab(RootScreenTab.Settings) },
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
                VerticalDivider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(),
                    color = CustomColors.MenuDividerColor,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                content(
                    windowInsets.asWindowInsets()
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        .asPaddingValues(),
                )
            }
        }
        if (LocalIsLargeScreen.current.not()) {
            NavigationBar(
                windowInsets = windowInsets.asWindowInsets()
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ) {
                NavigationBarItem(
                    selected = currentScreen == RootScreenTab.Home,
                    onClick = { onClickTab(RootScreenTab.Home) },
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
                    onClick = { onClickTab(RootScreenTab.List) },
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
                    onClick = { onClickTab(RootScreenTab.Add) },
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
                    onClick = { onClickTab(RootScreenTab.Settings) },
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
}
