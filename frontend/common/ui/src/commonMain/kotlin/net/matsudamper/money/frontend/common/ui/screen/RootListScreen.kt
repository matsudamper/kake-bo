package net.matsudamper.money.frontend.common.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class RootListScreenUiState(
    val event: Event,
) {
    public interface Event {
        public fun onClickAdd()
    }
}

@Composable
public fun RootListScreen(
    modifier: Modifier = Modifier,
    uiState: RootListScreenUiState,
    listener: RootScreenScaffoldListener,
) {
    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.List,
        listener = listener,
        content = {
            Box(Modifier.fillMaxSize()) {
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

                FloatingActionButton(
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(bottom = 24.dp, end = 24.dp),
                    onClick = { uiState.event.onClickAdd() },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "add money usage",
                    )
                }
            }
        },
    )
}
