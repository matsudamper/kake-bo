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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.AppRoot
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.base.RootScreenScaffold
import net.matsudamper.money.frontend.common.ui.generated.resources.Res
import net.matsudamper.money.frontend.common.ui.generated.resources.ic_arrow_back
import net.matsudamper.money.frontend.common.ui.layout.TextFieldType
import net.matsudamper.money.frontend.common.ui.layout.html.text.fullscreen.FullScreenTextInput
import net.matsudamper.money.frontend.common.ui.rememberCustomFontFamily
import org.jetbrains.compose.resources.painterResource

public data class AiSettingScreenUiState(
    val loadingState: LoadingState,
    val isGeminiApiKeyInputVisible: Boolean,
    val errorDialog: ErrorDialog?,
    val kakeboScaffoldListener: KakeboScaffoldListener,
    val event: Event,
) {
    @Immutable
    public sealed interface LoadingState {
        public data object Error : LoadingState
        public data object Loading : LoadingState
        public data class Loaded(
            val geminiApiKey: String,
        ) : LoadingState
    }

    public data class ErrorDialog(
        val message: String,
        val event: Event,
    ) {
        @Immutable
        public interface Event {
            public fun onDismiss()
        }
    }

    @Immutable
    public interface Event {
        public fun onClickBack()
        public fun onResume()
        public fun onClickRetry()
        public fun onClickChangeGeminiApiKey()
        public fun onCompleteGeminiApiKeyInput(text: String)
        public fun onCancelGeminiApiKeyInput()
    }
}

@Composable
public fun AiSettingScreen(
    modifier: Modifier = Modifier,
    uiState: AiSettingScreenUiState,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onResume()
    }
    if (uiState.isGeminiApiKeyInputVisible) {
        FullScreenTextInput(
            title = "Gemini API Key",
            default = "",
            inputType = TextFieldType.Password,
            onComplete = { uiState.event.onCompleteGeminiApiKeyInput(it) },
            canceled = { uiState.event.onCancelGeminiApiKeyInput() },
        )
    }
    uiState.errorDialog?.also { errorDialog ->
        AlertDialog(
            onDismissRequest = { errorDialog.event.onDismiss() },
            confirmButton = {
                TextButton(onClick = { errorDialog.event.onDismiss() }) {
                    Text("OK")
                }
            },
            title = { Text("エラー") },
            text = { Text(errorDialog.message) },
        )
    }

    RootScreenScaffold(
        modifier = modifier,
        windowInsets = windowInsets,
        topBar = {
            KakeBoTopAppBar(
                navigation = {
                    IconButton(onClick = { uiState.event.onClickBack() }) {
                        Icon(painter = painterResource(Res.drawable.ic_arrow_back), contentDescription = null)
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
                Text("AI設定")
            },
        ) { paddingValues ->
            when (val loadingState = uiState.loadingState) {
                AiSettingScreenUiState.LoadingState.Error -> {
                    LoadingErrorContent(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        onClickRetry = { uiState.event.onClickRetry() },
                    )
                }

                AiSettingScreenUiState.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is AiSettingScreenUiState.LoadingState.Loaded -> {
                    LoadedContent(
                        modifier = Modifier.fillMaxWidth()
                            .padding(paddingValues)
                            .padding(vertical = 24.dp),
                        uiState = loadingState,
                        event = uiState.event,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    modifier: Modifier = Modifier,
    uiState: AiSettingScreenUiState.LoadingState.Loaded,
    event: AiSettingScreenUiState.Event,
) {
    Column(modifier = modifier) {
        SettingsChangeTextSection(
            title = {
                Text("Gemini API Key")
            },
            text = {
                Text(
                    text = uiState.geminiApiKey,
                    fontFamily = rememberCustomFontFamily(),
                )
            },
            onClickChange = { event.onClickChangeGeminiApiKey() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "メール画面のAI解析に使用します。空で保存すると削除します",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
@Preview
private fun AiSettingScreenPreview() {
    AiSettingScreenPreviewContent(
        isGeminiApiKeyInputVisible = false,
        errorDialog = null,
    )
}

@Composable
@Preview
private fun AiSettingScreenErrorDialogPreview() {
    AiSettingScreenPreviewContent(
        isGeminiApiKeyInputVisible = true,
        errorDialog = AiSettingScreenUiState.ErrorDialog(
            message = "Gemini API Keyの更新に失敗しました",
            event = object : AiSettingScreenUiState.ErrorDialog.Event {
                override fun onDismiss() {}
            },
        ),
    )
}

@Composable
private fun AiSettingScreenPreviewContent(
    isGeminiApiKeyInputVisible: Boolean,
    errorDialog: AiSettingScreenUiState.ErrorDialog?,
) {
    AppRoot {
        AiSettingScreen(
            uiState = AiSettingScreenUiState(
                loadingState = AiSettingScreenUiState.LoadingState.Loaded(
                    geminiApiKey = "****************",
                ),
                isGeminiApiKeyInputVisible = isGeminiApiKeyInputVisible,
                errorDialog = errorDialog,
                kakeboScaffoldListener = object : KakeboScaffoldListener {
                    override fun onClickTitle() {}
                },
                event = object : AiSettingScreenUiState.Event {
                    override fun onClickBack() {}
                    override fun onResume() {}
                    override fun onClickRetry() {}
                    override fun onClickChangeGeminiApiKey() {}
                    override fun onCompleteGeminiApiKeyInput(text: String) {}
                    override fun onCancelGeminiApiKeyInput() {}
                },
            ),
            windowInsets = PaddingValues(0.dp),
        )
    }
}
