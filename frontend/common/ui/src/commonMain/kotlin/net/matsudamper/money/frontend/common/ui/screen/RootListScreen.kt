package net.matsudamper.money.frontend.common.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

@Composable
public fun RootListScreen(
    modifier: Modifier = Modifier,
    listener: RootScreenScaffoldListener,
) {
    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.List,
        listener = listener,
        content = {
            Column(Modifier.fillMaxSize()) {
                (0..4).forEach {
                    Card(modifier = Modifier.padding(32.dp)) {
                        Text(
                            modifier = Modifier.padding(32.dp),
                            text = "List",
                            fontFamily = rememberCustomFontFamily(),
                        )
                    }
                }
            }
        },
    )
}
