package net.matsudamper.money.frontend.common.ui.screen.imported_mail_content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.base.LoadingErrorContent
import net.matsudamper.money.frontend.common.ui.layout.html.html.Html

public data class ImportedMailContentScreenUiState(
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ImportedMailContentScreen(
    modifier: Modifier = Modifier,
    uiState: ImportedMailContentScreenUiState,
) {
    when(uiState.loadingState) {
        is ImportedMailContentScreenUiState.LoadingState.Loaded -> {
            Html(
                html = uiState.loadingState.html,
                onDismissRequest = {
                    uiState.event.onClickClose()
                }
            )
        }
        is ImportedMailContentScreenUiState.LoadingState.Loading -> {
            Scaffold(
                modifier = modifier,
                topBar = {
                    KakeBoTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                uiState.event.onClickClose()
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close")
                            }
                        }
                    ) {
                        Text("家計簿")
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        ImportedMailContentScreenUiState.LoadingState.Error -> {
            LoadingErrorContent(
                modifier = modifier,
                onClickRetry = {
                    uiState.event.onClickRetry()
                }
            )
        }
    }
}