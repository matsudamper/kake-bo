package net.matsudamper.money.frontend.common.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import RootHomeScreenUiState
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily


@Composable
public fun RootScreen(
    uiState: RootHomeScreenUiState,
    listener: RootScreenScaffoldListener,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
            )
        }
    } else {
        RootScreenScaffold(
            modifier = Modifier.fillMaxSize(),
            currentScreen = Screen.Root.Home,
            listener = listener,
            content = {
                Column(Modifier.fillMaxSize()) {
                    (0..4).forEach {
                        Card(modifier = Modifier.padding(32.dp)) {
                            Text(
                                modifier = Modifier.padding(32.dp),
                                text = "text",
                                fontFamily = rememberCustomFontFamily(),
                            )
                        }
                    }
                }
            },
        )
    }
}
