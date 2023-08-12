package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily

public data class ImapSettingScreenUiState(
    val textInputEvents: ImmutableList<TextInputUiState>,
    val loadingState: LoadingState,
    val event: Event,
) {
    @Immutable
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public data class Loaded(
            val imapConfig: ImapConfig,
        ) : LoadingState
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
        public val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun complete(text: String, event: TextInputUiState)
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
    listener: RootScreenScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }
    val lastEvent = uiState.textInputEvents.lastOrNull()
    if (lastEvent != null) {
        HtmlFullScreenTextInput(
            title = lastEvent.title,
            default = lastEvent.default,
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
        SettingElementContent(
            modifier = Modifier.fillMaxWidth()
                .padding(paddingValues)
                .padding(vertical = 24.dp),
            uiState = uiState.imapConfig,
        )
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
