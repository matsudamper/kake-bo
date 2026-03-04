package net.matsudamper.money.frontend.common.ui.screen.importedmail.plain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffold
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.html.html.HtmlText

public data class ImportedMailPlainScreenUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public object Loading : LoadingState

        public object Error : LoadingState

        public data class Loaded(
            val html: String,
            val translationState: TranslationState,
        ) : LoadingState
    }

    public sealed interface TranslationState {
        public object NotTranslated : TranslationState

        public object Loading : TranslationState

        public object Error : TranslationState

        public data class Translated(
            val translatedHtml: String,
            val sourceLanguage: String,
            val targetLanguage: String,
        ) : TranslationState
    }

    public interface Event {
        public fun onClickClose()

        public fun onClickRetry()

        public fun onViewInitialized()

        public fun onClickTranslate()
    }
}

@Composable
public fun ImportedMailPlainScreen(
    modifier: Modifier = Modifier,
    uiState: ImportedMailPlainScreenUiState,
    kakeboScaffoldListener: KakeboScaffoldListener,
    windowInsets: PaddingValues,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    when (uiState.loadingState) {
        is ImportedMailPlainScreenUiState.LoadingState.Loaded -> {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp)
                    .padding(windowInsets),
            ) {
                TranslationSection(
                    translationState = uiState.loadingState.translationState,
                    onClickTranslate = { uiState.event.onClickTranslate() },
                )
                Spacer(modifier = Modifier.height(8.dp))
                val displayHtml = when (val ts = uiState.loadingState.translationState) {
                    is ImportedMailPlainScreenUiState.TranslationState.Translated -> ts.translatedHtml
                    else -> uiState.loadingState.html
                }
                HtmlText(
                    html = displayHtml,
                    onDismissRequest = {
                        uiState.event.onClickClose()
                    },
                )
            }
        }

        is ImportedMailPlainScreenUiState.LoadingState.Loading -> {
            KakeboScaffold(
                modifier = modifier,
                windowInsets = windowInsets,
                topBar = {
                    KakeBoTopAppBar(
                        navigation = {
                            IconButton(
                                onClick = {
                                    uiState.event.onClickClose()
                                },
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        },
                        title = {
                            Text(
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    kakeboScaffoldListener.onClickTitle()
                                },
                                text = "家計簿",
                            )
                        },
                        windowInsets = windowInsets,
                    )
                },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(it),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        ImportedMailPlainScreenUiState.LoadingState.Error -> {
            LoadingErrorContent(
                modifier = modifier.padding(windowInsets),
                onClickRetry = {
                    uiState.event.onClickRetry()
                },
            )
        }
    }
}

@Composable
private fun TranslationSection(
    translationState: ImportedMailPlainScreenUiState.TranslationState,
    onClickTranslate: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (translationState) {
                ImportedMailPlainScreenUiState.TranslationState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(text = "翻訳中...")
                }
                ImportedMailPlainScreenUiState.TranslationState.NotTranslated -> {
                    OutlinedButton(onClick = onClickTranslate) {
                        Text("日本語に翻訳")
                    }
                }
                ImportedMailPlainScreenUiState.TranslationState.Error -> {
                    Text(
                        text = "翻訳に失敗しました",
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = onClickTranslate,
                    ) {
                        Text("再試行")
                    }
                }
                is ImportedMailPlainScreenUiState.TranslationState.Translated -> {
                    Text(
                        text = "${translationState.sourceLanguage} → ${translationState.targetLanguage}  に翻訳済み",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
