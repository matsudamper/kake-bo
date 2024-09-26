package net.matsudamper.money.frontend.common.ui.screen.root.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.RootScreenTab
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.HtmlFullScreenTextInput

public data class ApiSettingScreenUiState(
    val loadingState: LoadingState,
    val addDialog: AddDialogUiState?,
    val addTokenResult: AddTokenResult?,
    val event: Event,
) {
    public data class AddTokenResult(
        val name: String,
        val event: Event,
    ) {
        public interface Event {
            public fun dismiss()
            public fun onClickCopy()
        }
    }

    public data class AddDialogUiState(
        val event: Event,
    ) {
        public interface Event {
            public fun onComplete(name: String)
            public fun dismissRequest()
        }
    }

    @Immutable
    public sealed interface LoadingState {
        public data object Error : LoadingState
        public data class Loaded(
            val tokens: List<Token>,
            val event: LoadedEvent,
        ) : LoadingState

        public data object Loading : LoadingState
    }

    @Immutable
    public interface LoadedEvent {
        public fun onClickAddToken()
    }

    public data class Token(
        val name: String,
        val expiresAt: String,
    )

    @Immutable
    public interface Event {
        public fun onClickBack()
        public suspend fun onViewInitialized()
        public fun onClickReloadButton()
    }
}

@Composable
public fun ApiSettingScreen(
    modifier: Modifier = Modifier,
    uiState: ApiSettingScreenUiState,
    rootScreenScaffoldListener: RootScreenScaffoldListener,
    snackbarHostState: SnackbarHostState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    if (uiState.addDialog != null) {
        Dialog(onDismissRequest = { uiState.addDialog.event.dismissRequest() }) {
            HtmlFullScreenTextInput(
                title = "トークンを追加",
                onComplete = { uiState.addDialog.event.onComplete(it) },
                canceled = { uiState.addDialog.event.dismissRequest() },
                default = "",
            )
        }
    }

    if (uiState.addTokenResult != null) {
        Dialog(onDismissRequest = { uiState.addTokenResult.event.dismiss() }) {
            Card(
                modifier = Modifier.padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        text = "トークンを追加しました",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f)
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                text = uiState.addTokenResult.name,
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(onClick = { uiState.addTokenResult.event.onClickCopy() }) {
                            Text("COPY")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { uiState.addTokenResult.event.dismiss() }) {
                            Text("閉じる")
                        }
                    }
                }
            }
        }
    }

    RootScreenScaffold(
        modifier = modifier,
        currentScreen = RootScreenTab.Settings,
        snackbarHostState = snackbarHostState,
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
                windowInsets = windowInsets,
            )
        },
        listener = rootScreenScaffoldListener,
    ) {
        SettingScaffold(
            title = {
                Text("API設定")
            },
        ) { paddingValues ->
            when (val loadingState = uiState.loadingState) {
                ApiSettingScreenUiState.LoadingState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("エラーが発生しました")
                        OutlinedButton(onClick = { uiState.event.onClickReloadButton() }) {
                            Text("再読み込み")
                        }
                    }
                }

                is ApiSettingScreenUiState.LoadingState.Loaded -> {
                    Content(
                        modifier = Modifier.fillMaxSize(),
                        paddingValues = paddingValues,
                        uiState = loadingState,
                    )
                }

                ApiSettingScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    uiState: ApiSettingScreenUiState.LoadingState.Loaded,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = "トークン一覧",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = { uiState.event.onClickAddToken() }) {
                Text("トークンを追加")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = paddingValues,
        ) {
            items(uiState.tokens) { token ->
                Card(
                    modifier = Modifier.padding(8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Row {
                            Text("トークン名: ")
                            Text(text = token.name)
                        }
                        Row {
                            Text(text = "有効期限: ")
                            Text(text = token.expiresAt)
                        }
                    }
                }
            }
        }
    }
}
