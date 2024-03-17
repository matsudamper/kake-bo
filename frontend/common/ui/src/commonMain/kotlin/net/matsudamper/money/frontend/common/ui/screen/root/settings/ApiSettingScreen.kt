package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab

public data class ApiSettingScreenUiState(
    val event: Event,
) {
    public interface Event {
        public fun onClickBack()
    }
}

@Composable
public fun ApiSettingScreen(
    modifier: Modifier = Modifier,
    uiState: ApiSettingScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
) {
    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Settings,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier =
                        Modifier.clickable(
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        listener = rootScreenScaffoldListener,
    ) {
        SettingScaffold(
            title = {
                Text("API設定")
            },
        ) {

        }
    }
}
