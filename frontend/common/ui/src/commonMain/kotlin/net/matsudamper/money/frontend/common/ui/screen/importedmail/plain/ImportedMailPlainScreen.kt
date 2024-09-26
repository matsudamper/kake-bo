package net.matsudamper.money.frontend.common.ui.screen.importedmail.plain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffold
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.html.html.Html

public data class ImportedMailPlainScreenUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public object Loading : LoadingState

        public object Error : LoadingState

        public data class Loaded(
            val html: String,
        ) : LoadingState
    }

    public interface Event {
        public fun onClickClose()

        public fun onClickRetry()

        public fun onViewInitialized()
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
            Html(
                html = uiState.loadingState.html,
                onDismissRequest = {
                    uiState.event.onClickClose()
                },
            )
        }

        is ImportedMailPlainScreenUiState.LoadingState.Loading -> {
            KakeboScaffold(
                modifier = modifier.padding(windowInsets),
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
                                modifier =
                                Modifier.clickable(
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
                    modifier =
                    Modifier.fillMaxSize()
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
