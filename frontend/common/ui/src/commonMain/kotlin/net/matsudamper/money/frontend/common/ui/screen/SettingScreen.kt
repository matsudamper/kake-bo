package net.matsudamper.money.frontend.common.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.matsudamper.money.frontend.common.base.Screen
import net.matsudamper.money.frontend.common.base.rememberCustomFontFamily
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput
import net.matsudamper.root.RootSettingScreenUiState

@Composable
public fun RootSettingScreen(
    modifier: Modifier = Modifier,
    uiState: RootSettingScreenUiState,
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
        currentScreen = Screen.Root.Settings,
        listener = listener,
        content = {
            when (val loadingState = uiState.loadingState) {
                is RootSettingScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        uiState = loadingState,
                    )
                }

                is RootSettingScreenUiState.LoadingState.Loading -> {
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
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: RootSettingScreenUiState.LoadingState.Loaded,
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
                SettingElementContent(
                    modifier = Modifier.fillMaxWidth(),
                    uiState = uiState.imapConfig,
                )
            }
        }
    }
}

@Composable
private fun SettingSection(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            title()
        }
        Spacer(Modifier.height(8.dp))
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingElementContent(
    modifier: Modifier = Modifier,
    uiState: RootSettingScreenUiState.ImapConfig,
) {
    SettingSection(
        modifier = modifier,
        title = {
            Text(
                text = "IMAP設定",
                fontFamily = rememberCustomFontFamily(),
            )
        },
    ) {
        ChangeTextSection(
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
        ChangeTextSection(
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
        ChangeTextSection(
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
        ChangeTextSection(
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
private fun ChangeTextSection(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClickChange: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
            ) {
                title()
            }
            Spacer(Modifier.height(2.dp))
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                ),
            ) {
                text()
            }
        }
        OutlinedButton(
            onClick = { onClickChange() },
        ) {
            Text(
                text = "変更",
                fontFamily = rememberCustomFontFamily(),
            )
        }
    }
}
