package net.matsudamper.money.frontend.common.ui.screen.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.base.SharedNavigation

@Composable
public fun RootHomeTabScreenScaffold(
    scaffoldListener: RootScreenScaffoldListener,
    navigationUi: SharedNavigation,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit = {},
    windowInsets: PaddingValues,
    content: @Composable () -> Unit,
) {
    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Home,
        listener = scaffoldListener,
        navigationUi = navigationUi,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                windowInsets = windowInsets,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                scaffoldListener.kakeboScaffoldListener.onClickTitle()
                            },
                            text = "家計簿",
                        )
                        menu()
                    }
                },
                menu = {},
            )
        },
        content = {
            content()
        },
    )
}
