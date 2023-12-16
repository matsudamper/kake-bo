package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class LoginSettingScreenUiState(
    val event: Event,
) {
    @Immutable
    public interface Event {
        public fun onClickBack()
        public fun onClickPlatform()
        public fun onClickCrossPlatform()
    }
}

@Composable
public fun LoginSettingScreen(
    uiState: LoginSettingScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    modifier: Modifier = Modifier,
) {
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Settings,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            rootScreenScaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        listener = rootScreenScaffoldListener,
    ) {
        Column {
            Button(
                onClick = { uiState.event.onClickPlatform() },
            ) {
                Text("PLATFORM")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { uiState.event.onClickCrossPlatform() },
            ) {
                Text("CROSS_PLATFORM")
            }
        }
    }
}
