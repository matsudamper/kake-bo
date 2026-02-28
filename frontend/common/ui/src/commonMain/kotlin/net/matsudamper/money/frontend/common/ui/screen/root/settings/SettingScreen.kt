package net.matsudamper.money.frontend.common.ui.screen.root.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LocalScrollToTopHandler
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold

public data class RootSettingScreenUiState(
    val kotlinVersion: String,
    val kakeboScaffoldListener: KakeboScaffoldListener,
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
        public fun onClickTextFieldTest()
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
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                title = {
                    Text(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            uiState.kakeboScaffoldListener.onClickTitle()
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
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
            val scrollToTopHandler = LocalScrollToTopHandler.current
            DisposableEffect(scrollToTopHandler, scrollState) {
                val handler = {
                    if (scrollState.value > 0) {
                        coroutineScope.launch { scrollState.animateScrollTo(0) }
                        true
                    } else {
                        false
                    }
                }
                scrollToTopHandler.register(handler)
                onDispose { scrollToTopHandler.unregister() }
            }
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
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "外部アプリで開く",
                            )
                        },
                    ) {
                        Text("GitHub")
                    }
                    SettingListMenuItemButton(
                        onClick = { uiState.event.onClickTextFieldTest() },
                    ) {
                        Text("TextFieldテスト")
                    }
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
            kakeboScaffoldListener = object : KakeboScaffoldListener {
                override fun onClickTitle() {}
            },
            event = object : RootSettingScreenUiState.Event {
                override fun onResume() {}
                override fun onClickImapButton() {}
                override fun onClickCategoryButton() {}
                override fun onClickMailFilter() {}
                override fun onClickGitHub() {}
                override fun onClickLoginSetting() {}
                override fun onClickApiSetting() {}
                override fun onClickTextFieldTest() {}
            },
        ),
        windowInsets = PaddingValues(),
    )
}
