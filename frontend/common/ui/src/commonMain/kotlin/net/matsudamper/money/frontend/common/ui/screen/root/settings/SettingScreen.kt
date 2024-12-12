package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import org.jetbrains.compose.ui.tooling.preview.Preview

public data class RootSettingScreenUiState(
    val kotlinVersion: String,
    val rootScreenScaffoldListener: RootScreenScaffoldListener,
    val event: Event,
) {
    public interface Event {
        public fun onResume()

        public fun onClickImapButton()

        public fun onClickCategoryButton()

        public fun onClickMailFilter()

        public fun onClickGitHub()

        public fun onClickLoginSetting()
        public fun onClickApiSetting()
    }
}

@Composable
public fun SettingRootScreen(
    modifier: Modifier = Modifier,
    uiState: RootSettingScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
        currentScreen = RootScreenTab.Settings,
        listener = uiState.rootScreenScaffoldListener,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier =
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.rootScreenScaffoldListener.kakeboScaffoldListener.onClickTitle()
                        },
                        text = "家計簿",
                    )
                },
                windowInsets = windowInsets,
            )
        },
        content = {
            SettingScaffold(
                modifier = Modifier.fillMaxSize(),
                title = {
                    Text(text = "設定")
                },
            ) {
                MainContent(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                )
            }
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
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier =
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val scrollState = rememberScrollState()
            var scrollContainerHeightPx by remember { mutableIntStateOf(0) }
            Column(
                Modifier
                    .then(settingPaddingModifier)
                    .padding(horizontal = 8.dp)
                    .fillMaxSize()
                    .onSizeChanged {
                        scrollContainerHeightPx = it.height
                    }
                    .verticalScroll(scrollState),
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
                        onClick = { uiState.event.onClickLoginSetting() },
                    ) {
                        Text("ログイン設定")
                    }
                    SettingListMenuItemButton(
                        onClick = { uiState.event.onClickApiSetting() },
                    ) {
                        Text("API設定")
                    }
                    SettingListMenuItemButton(
                        onClick = { uiState.event.onClickGitHub() },
                    ) {
                        Text("GitHub")
                    }
                    Text("TextField2")
                    BasicTextField(
                        modifier = Modifier.fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            .padding(12.dp),
                        state = rememberTextFieldState(),
                        textStyle = MaterialTheme.typography.bodyLarge.merge(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                    Text("TextField")
                    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
                    TextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                        },
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Kotlin version: ${uiState.kotlinVersion}",
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    SettingRootScreen(
        uiState = RootSettingScreenUiState(
            kotlinVersion = "preview",
            rootScreenScaffoldListener = RootScreenScaffoldListener.previewImpl,
            event = object : RootSettingScreenUiState.Event {
                override fun onResume() {}
                override fun onClickImapButton() {}
                override fun onClickCategoryButton() {}
                override fun onClickMailFilter() {}
                override fun onClickGitHub() {}
                override fun onClickLoginSetting() {}
                override fun onClickApiSetting() {}
            },
        ),
        windowInsets = PaddingValues(),
    )
}
