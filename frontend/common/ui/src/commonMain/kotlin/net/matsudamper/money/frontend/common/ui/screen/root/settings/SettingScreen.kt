package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.ScrollButtons
import net.matsudamper.money.frontend.common.ui.ScrollButtonsDefaults
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import org.jetbrains.compose.ui.tooling.preview.Preview

public data class RootSettingScreenUiState(
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
                        modifier =
                        Modifier.clickable(
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
                        state = rememberTextFieldState(),
                    )
                    Text("TextField")
                    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
                    TextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                        },
                    )
                }
            }

            ScrollButtons(
                modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ScrollButtonsDefaults.padding)
                    .height(ScrollButtonsDefaults.height),
                scrollState = scrollState,
                scrollSize = scrollContainerHeightPx * 0.4f,
            )
        }
    }
}

@Composable
@Preview
private fun Preview() {
    SettingRootScreen(
        uiState = RootSettingScreenUiState(
            object : RootSettingScreenUiState.Event {
                override fun onResume() {}
                override fun onClickImapButton() {}
                override fun onClickCategoryButton() {}
                override fun onClickMailFilter() {}
                override fun onClickGitHub() {}
                override fun onClickLoginSetting() {}
                override fun onClickApiSetting() {}
            },
        ),
        listener = object : RootScreenScaffoldListener {
            override val kakeboScaffoldListener: KakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {}
            }
            override fun onClickHome() {}
            override fun onClickList() {}
            override fun onClickSettings() {}
            override fun onClickAdd() {}
        },
    )
}
