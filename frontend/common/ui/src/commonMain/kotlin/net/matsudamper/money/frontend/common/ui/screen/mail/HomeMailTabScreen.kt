package net.matsudamper.money.frontend.common.ui.screen.mail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.matsudamper.money.frontend.common.base.ImmutableList
import net.matsudamper.money.frontend.common.ui.base.KakeBoTopAppBar
import net.matsudamper.money.frontend.common.ui.layout.GridColumn

public data class MailScreenUiState(
    val loadingState: LoadingState,
    val event: Event,
) {
    public sealed interface LoadingState {
        public object Loading : LoadingState
        public data class Loaded(
            val mail: Mail,
            val usageSuggest: ImmutableList<UsageSuggest>,
        ) : LoadingState
    }

    public data class Mail(
        val from: String,
        val title: String,
        val date: String,
    )

    public data class UsageSuggest(
        val title: String,
        val amount: String?,
        val category: String?,
        val description: String,
    )

    @Immutable
    public interface Event {

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MailScreen(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            KakeBoTopAppBar {
                Text(
                    text = when (uiState.loadingState) {
                        is MailScreenUiState.LoadingState.Loaded -> uiState.loadingState.mail.title
                        is MailScreenUiState.LoadingState.Loading -> ""
                    }
                )
            }
        },
    ) { paddingValues ->
        when (uiState.loadingState) {
            is MailScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.padding(paddingValues)
                        .fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            is MailScreenUiState.LoadingState.Loaded -> {
                MainContent(
                    modifier = Modifier.padding(paddingValues),
                    uiState = uiState.loadingState,
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier,
    uiState: MailScreenUiState.LoadingState.Loaded,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .padding(24.dp)
            .widthIn(max = 700.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "メール",
            style = MaterialTheme.typography.titleLarge,
        )
        Divider(modifier = Modifier.fillMaxWidth().height(1.dp))
        MailCard(
            modifier = Modifier.fillMaxWidth(),
            uiState = uiState.mail,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MailCard(
    modifier: Modifier = Modifier,
    uiState: MailScreenUiState.Mail,
) {
    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp),
        ) {
            GridColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalPadding = 8.dp,
                verticalPadding = 4.dp,
            ) {
                row {
                    item {
                        Text("From")
                    }
                    item {
                        Text(text = uiState.from)
                    }
                }
                row {
                    item {
                        Text("タイトル")
                    }
                    item {
                        Text(text = uiState.title)
                    }
                }
                row {
                    item {
                        Text("日付")
                    }
                    item {
                        Text(text = uiState.date)
                    }
                }
            }
        }
    }
}
