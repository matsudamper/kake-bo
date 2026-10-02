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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.base.immutableListOf
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

public data class ImapSettingScreenUiState(
    val textInputEvents: ImmutableList<TextInputUiState>,
    val loadingState: LoadingState,
    val event: Event,
    val kakeboScaffoldListener: KakeboScaffoldListener,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState

        public data class Loaded(
            val imapConfig: ImapConfig,
            val geminiConfig: GeminiConfig,
        ) : LoadingState
    }

    public data class GeminiConfig(
        val apiKey: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onClickChangeApiKey()
        }
    }

    public data class ImapConfig(
        val host: String,
        val userName: String,
        val port: String,
        val password: String,
        val event: Event,
    ) {
        public interface Event {
            public fun onClickChangeHost()

            public fun onClickChangeUserName()

            public fun onClickChangePort()

            public fun onClickChangePassword()
        }
    }

    @Immutable
    public class TextInputUiState(
        public val title: String,
        public val default: String,
        public val inputType: TextFieldType,
        public val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun complete(
                text: String,
                event: TextInputUiState,
            )

            public fun cancel(event: TextInputUiState)
        }
    }

    public interface Event {
        public fun consumeTextInputEvent(event: TextInputUiState)

        public fun onResume()
    }
}

@Composable
public fun ImapConfigScreen(
    modifier: Modifier = Modifier,
    uiState: ImapSettingScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }
    val lastEvent = uiState.textInputEvents.lastOrNull()
    if (lastEvent != null) {
        FullScreenTextInput(
            title = lastEvent.title,
            default = lastEvent.default,
            inputType = lastEvent.inputType,
            onComplete = {
                lastEvent.event.complete(
                    text = it,
                    event = lastEvent,
                )
            },
            canceled = {
                lastEvent.event.cancel(lastEvent)
            },
        )
    }
    RootScreenScaffold(
        modifier = modifier.fillMaxSize(),
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
        windowInsets = windowInsets,
        content = {
            when (val loadingState = uiState.loadingState) {
                is ImapSettingScreenUiState.LoadingState.Loaded -> {
                    MainContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = loadingState,
                    )
                }

                is ImapSettingScreenUiState.LoadingState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
    )
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    uiState: ImapSettingScreenUiState.LoadingState.Loaded,
) {
    SettingScaffold(
        modifier = modifier.verticalScroll(rememberScrollState()),
        title = {
            Text(
                text = "IMAP設定",
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(paddingValues)
                .padding(vertical = 24.dp),
        ) {
            SettingElementContent(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState.imapConfig,
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Gemini API設定",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(14.dp))
            SettingsChangeTextSection(
                title = {
                    Text("API Key")
                },
                text = {
                    Text(
                        text = uiState.geminiConfig.apiKey,
                        fontFamily = rememberCustomFontFamily(),
                    )
                },
                onClickChange = { uiState.geminiConfig.event.onClickChangeApiKey() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "メール画面のAI解析に使用します。空で保存すると削除します",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SettingElementContent(
    modifier: Modifier = Modifier,
    uiState: ImapSettingScreenUiState.ImapConfig,
) {
    Column(modifier = modifier) {
        SettingsChangeTextSection(
            title = {
                Text("Host")
            },
            text = {
                Text(
                    text = uiState.host,
                    fontFamily = rememberCustomFontFamily(),
                )
            },
            onClickChange = { uiState.event.onClickChangeHost() },
        )
        Spacer(Modifier.height(14.dp))
        SettingsChangeTextSection(
            title = {
                Text("User Name")
            },
            text = {
                Text(
                    text = uiState.userName,
                    fontFamily = rememberCustomFontFamily(),
                )
            },
            onClickChange = { uiState.event.onClickChangeUserName() },
        )
        Spacer(Modifier.height(14.dp))
        SettingsChangeTextSection(
            title = {
                Text("Port")
            },
            text = {
                Text(
                    text = uiState.port,
                    fontFamily = rememberCustomFontFamily(),
                )
            },
            onClickChange = { uiState.event.onClickChangePort() },
        )
        Spacer(Modifier.height(14.dp))
        SettingsChangeTextSection(
            title = {
                Text("Password")
            },
            text = {
                Text(
                    text = uiState.password,
                    fontFamily = rememberCustomFontFamily(),
                )
            },
            onClickChange = { uiState.event.onClickChangePassword() },
        )
    }
}

@Composable
@Preview
private fun ImapConfigScreenPreview() {
    AppRoot {
        ImapConfigScreen(
            uiState = ImapSettingScreenUiState(
                textInputEvents = immutableListOf(),
                loadingState = ImapSettingScreenUiState.LoadingState.Loaded(
                    imapConfig = ImapSettingScreenUiState.ImapConfig(
                        host = "imap.example.com",
                        userName = "user@example.com",
                        port = "993",
                        password = "****************",
                        event = object : ImapSettingScreenUiState.ImapConfig.Event {
                            override fun onClickChangeHost() {}
                            override fun onClickChangeUserName() {}
                            override fun onClickChangePort() {}
                            override fun onClickChangePassword() {}
                        },
                    ),
                    geminiConfig = ImapSettingScreenUiState.GeminiConfig(
                        apiKey = "****************",
                        event = object : ImapSettingScreenUiState.GeminiConfig.Event {
                            override fun onClickChangeApiKey() {}
                        },
                    ),
                ),
                event = object : ImapSettingScreenUiState.Event {
                    override fun consumeTextInputEvent(event: ImapSettingScreenUiState.TextInputUiState) {}
                    override fun onResume() {}
                },
                kakeboScaffoldListener = object : KakeboScaffoldListener {
                    override fun onClickTitle() {}
                },
            ),
            windowInsets = PaddingValues(0.dp),
        )
    }
}
