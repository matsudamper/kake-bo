package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput

public data class TimezoneSettingScreenUiState(
    val loadingState: LoadingState,
    val textInputEvent: TextInputUiState?,
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val event: Event,
) {
    @Immutable
    public sealed interface LoadingState {
        public data object Error : LoadingState
        public data object Loading : LoadingState
        public data class Loaded(
            val timezoneOffsetMinutes: Int,
            val timezoneOffsetText: String,
            val event: LoadedEvent,
        ) : LoadingState
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickChange()
    }

    @Immutable
    public interface Event {
        public fun onClickBack()
        public fun onResume()
        public fun onClickRetry()
        public fun consumeTextInputEvent()
    }

    public data class TextInputUiState(
        val title: String,
        val default: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun complete(text: String)
            public fun cancel()
        }
    }
}

@Composable
public fun TimezoneSettingScreen(
    modifier: Modifier = Modifier,
    uiState: TimezoneSettingScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }

    if (uiState.textInputEvent != null) {
        Dialog(
            onDismissRequest = {
                uiState.textInputEvent.event.cancel()
                uiState.event.consumeTextInputEvent()
            },
        ) {
            FullScreenTextInput(
                title = uiState.textInputEvent.title,
                onComplete = { uiState.textInputEvent.event.complete(it) },
                canceled = { uiState.textInputEvent.event.cancel() },
                default = uiState.textInputEvent.default,
            )
        }
    }

    RootScreenScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
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
    ) {
        SettingScaffold(
            title = {
                Text("タイムゾーン設定")
            },
        ) { paddingValues ->
            when (val loadingState = uiState.loadingState) {
                TimezoneSettingScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        onClickRetry = { uiState.event.onClickRetry() },
                    )
                }

                TimezoneSettingScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TimezoneSettingScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        paddingValues = paddingValues,
                        uiState = loadingState,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    uiState: TimezoneSettingScreenUiState.LoadingState.Loaded,
) {
    Column(
        modifier = modifier.padding(paddingValues).padding(16.dp),
    ) {
        Text(
            text = "タイムゾーン",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "メールの時刻が含まれていない場合、このオフセットをメール受信時刻に適用します。",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "現在の設定: ${uiState.timezoneOffsetText}",
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { uiState.event.onClickChange() }) {
                Text("変更")
            }
        }
    }
}
