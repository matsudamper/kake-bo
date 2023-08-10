package net.matsudamper.money.frontend.common.ui.screen.importedmail.html

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffold
import net.matsudamper.money.frontend.common.ui.base.KakeboScaffoldListener
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.html.html.Html

public data class ImportedMailHtmlScreenUiState(
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
public fun ImportedMailHtmlScreen(
    modifier: Modifier = Modifier,
    uiState: ImportedMailHtmlScreenUiState,
    kakeboScaffoldListener: KakeboScaffoldListener,
) {
    LaunchedEffect(Unit) {
        uiState.event.onViewInitialized()
    }
    when (uiState.loadingState) {
        is ImportedMailHtmlScreenUiState.LoadingState.Loaded -> {
            Html(
                html = uiState.loadingState.html,
                onDismissRequest = {
                    uiState.event.onClickClose()
                },
            )
        }

        is ImportedMailHtmlScreenUiState.LoadingState.Loading -> {
            KakeboScaffold(
                modifier = modifier,
                listener = kakeboScaffoldListener,
                navigationIcon = {
                    IconButton(
                        onClick = {
                            uiState.event.onClickClose()
                        },
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
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

        ImportedMailHtmlScreenUiState.LoadingState.Error -> {
            LoadingErrorContent(
                modifier = modifier,
                onClickRetry = {
                    uiState.event.onClickRetry()
                },
            )
        }
    }
}
