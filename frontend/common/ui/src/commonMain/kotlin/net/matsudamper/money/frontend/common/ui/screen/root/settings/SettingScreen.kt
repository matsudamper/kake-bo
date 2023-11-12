package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

public data class RootSettingScreenUiState(
    val event: Event,
) {

    public interface Event {
        public fun onResume()
        public fun onClickImapButton()
        public fun onClickCategoryButton()
        public fun onClickMailFilter()
        public fun onClickGitHub()
    }
}

@Composable
public fun SettingRootScreen(
    modifier: Modifier = Modifier,
    uiState: RootSettingScreenUiState,
    listener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Settings,
        listener = listener,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            listener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
            )
        },
        content = {
            MainContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
            )
        },
    )
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    uiState: RootSettingScreenUiState,
) {
    Column(
        modifier = modifier,
    ) {
        val settingPaddingModifier = Modifier.padding(horizontal = 24.dp)
        Column(
            modifier = Modifier.then(settingPaddingModifier),
        ) {
            Text(
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 24.dp,
                ),
                text = "設定",
                fontFamily = rememberCustomFontFamily(),
            )
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            Modifier
                .then(settingPaddingModifier)
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 700.dp),
            ) {
                SettingListMenuItemButton(
                    onClick = { uiState.event.onClickImapButton() },
                ) {
                    Text("IMAP接続設定")
                }
                SettingListMenuItemButton(
                    onClick = { uiState.event.onClickCategoryButton() },
                ) {
                    Text("カテゴリ編集")
                }
                SettingListMenuItemButton(
                    onClick = { uiState.event.onClickMailFilter() },
                ) {
                    Text("メールフィルター")
                }
                SettingListMenuItemButton(
                    onClick = { uiState.event.onClickGitHub() },
                ) {
                    Text("GitHub")
                }
            }
        }
    }
}
